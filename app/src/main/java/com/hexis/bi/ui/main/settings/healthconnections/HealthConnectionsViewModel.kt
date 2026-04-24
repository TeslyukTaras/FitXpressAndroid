package com.hexis.bi.ui.main.settings.healthconnections

import android.app.Activity
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import co.tryterra.terra.InvalidAuthToken
import co.tryterra.terra.InvalidDevId
import co.tryterra.terra.NoInternet
import co.tryterra.terra.UnexpectedError
import co.tryterra.terra.UserLimitExceeded
import co.tryterra.terra.enums.Connections
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.R
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraCallbackHandler
import com.hexis.bi.data.terra.TerraConnector
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.terra.TerraWidgetApi
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.TerraProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber

class HealthConnectionsViewModel(
    application: Application,
    private val terraConnector: TerraConnector,
    private val terraWidgetApi: TerraWidgetApi,
    private val healthConnectionsRepository: HealthConnectionsRepository,
    private val terraCallbackHandler: TerraCallbackHandler,
    private val firebaseAuth: FirebaseAuth,
    private val terraApi: TerraApi,
    private val terraManagerHolder: TerraManagerHolder,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HealthConnectionsState())
    val state = _state.asStateFlow()

    init {
        healthConnectionsRepository.observeConnections()
            .onEach { items ->
                val active = items.filter { it.active }
                val (sdk, wearables) = active.partition { it.provider == TerraProviders.HEALTH_CONNECT }
                val connectedProviders = buildSet {
                    if (sdk.isNotEmpty()) add(HealthProvider.GoogleHealth)
                }
                _state.update {
                    it.copy(
                        connectedProviders = connectedProviders,
                        wearableConnections = wearables,
                    )
                }
            }
            .catch { setError(R.string.error_connection_save_failed) }
            .launchIn(viewModelScope)

        terraCallbackHandler.outcomes
            .onEach(::handleCallbackOutcome)
            .launchIn(viewModelScope)
    }

    fun onProviderClick(provider: HealthProvider, activity: Activity) {
        when (provider) {
            HealthProvider.GoogleHealth -> {
                val uiConnected = HealthProvider.GoogleHealth in _state.value.connectedProviders
                val sdkConnected =
                    terraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT) != null
                if (uiConnected || sdkConnected) {
                    disconnectGoogleHealth(activity)
                } else {
                    connectHealthConnect(activity)
                }
            }
            HealthProvider.AppleHealth -> Unit
        }
    }

    fun onOuraRowClick() {
        if (isWearableConnected(TerraProviders.OURA)) {
            disconnectWearableByProvider(TerraProviders.OURA, R.string.health_connection_oura)
        } else {
            onConnectOura()
        }
    }

    fun onDummyRowClick() {
        if (isWearableConnected(TerraProviders.DUMMY)) {
            disconnectWearableByProvider(TerraProviders.DUMMY, R.string.health_connection_dummy)
        } else {
            onConnectDummy()
        }
    }

    fun onConnectOura() = startWidgetSession(TerraProviders.OURA)

    fun onConnectDummy() = startWidgetSession(TerraProviders.DUMMY)

    private fun isWearableConnected(terraProvider: String): Boolean =
        _state.value.wearableConnections.any { it.provider.equals(terraProvider, ignoreCase = true) }

    private fun disconnectWearableByProvider(terraProvider: String, @StringRes nameRes: Int) = launch(
        onError = { setError(R.string.error_connection_save_failed) },
    ) {
        if (firebaseAuth.currentUser?.uid == null) {
            setError(R.string.error_sign_in_required)
            return@launch
        }
        val connections = healthConnectionsRepository.getConnections().getOrElse {
            setError(R.string.error_connection_save_failed)
            return@launch
        }
        val ids = connections
            .filter { it.active && it.provider.equals(terraProvider, ignoreCase = true) }
            .map { it.terraUserId }
            .distinct()
        if (ids.isEmpty()) {
            setMessage(R.string.msg_health_connect_nothing_to_disconnect)
            return@launch
        }
        val label = appContext.getString(nameRes)
        for (id in ids) {
            terraApi.deauthenticateUser(id).onFailure { e ->
                Timber.w(e, "Terra deauthenticateUser failed user_id=%s", id)
            }
        }
        for (id in ids) {
            healthConnectionsRepository.deactivateConnection(id).onFailure {
                Timber.w(it, "deactivateConnection failed for %s", id)
            }
        }
        setMessage(R.string.msg_wearable_disconnected, label)
    }

    private fun startWidgetSession(providers: String) = launch(
        onError = { setError(R.string.error_wearable_start_failed) },
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            setError(R.string.error_sign_in_required)
            return@launch
        }

        terraWidgetApi.generateWidgetSession(referenceId = uid, providers = providers)
            .onSuccess { url -> _state.update { it.copy(widgetUrl = url) } }
            .onFailure { setError(R.string.error_wearable_start_failed) }
    }

    fun onWidgetOpened() {
        _state.update { it.copy(widgetUrl = null) }
    }

    fun onWidgetLaunchFailed() {
        _state.update { it.copy(widgetUrl = null) }
        setError(R.string.error_wearable_start_failed)
    }

    /** Terra REST deauth, Firestore deactivate, clear local SDK, then re-run [TerraManagerHolder.init]. */
    fun disconnectGoogleHealth(activity: Activity) = launch(
        onError = { setError(R.string.error_health_connect_failed) },
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            setError(R.string.error_sign_in_required)
            return@launch
        }
        val connections = healthConnectionsRepository.getConnections().getOrElse {
            setError(R.string.error_connection_save_failed)
            return@launch
        }
        val hcFirestoreIds = connections
            .filter { it.active && it.provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) }
            .map { it.terraUserId }
            .distinct()
        val sdkId = terraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT)
        val allIds = (hcFirestoreIds + listOfNotNull(sdkId)).distinct()
        if (allIds.isEmpty()) {
            setMessage(R.string.msg_health_connect_nothing_to_disconnect)
            return@launch
        }
        for (id in allIds) {
            terraApi.deauthenticateUser(id).onFailure { e ->
                Timber.w(e, "Terra deauthenticateUser failed user_id=%s", id)
            }
        }
        for (id in hcFirestoreIds) {
            healthConnectionsRepository.deactivateConnection(id).onFailure {
                Timber.w(it, "deactivateConnection failed for %s", id)
            }
        }
        terraManagerHolder.clearLocalManager()
        terraManagerHolder.init(activity, uid)
            .onFailure { Timber.e(it, "Terra init after disconnect failed") }
        setMessage(R.string.msg_health_connect_disconnected)
    }

    private fun connectHealthConnect(activity: Activity) = launch(
        onError = { setError(it.toHealthConnectErrorRes()) },
    ) {
        val connected = terraConnector.connect(activity, Connections.HEALTH_CONNECT).getOrElse {
            setError(it.toHealthConnectErrorRes())
            return@launch
        }
        if (!connected) {
            setError(R.string.error_health_connect_failed)
            return@launch
        }
        terraManagerHolder.current?.let { mgr ->
            TerraSdkSync.syncLinkedConnections(
                mgr,
                reason = "post_connect",
                force = true,
            )
        }
        persistHealthConnectConnection()
        setMessage(R.string.msg_health_connect_connected)
    }

    @StringRes
    private fun Throwable.toHealthConnectErrorRes(): Int = when (this) {
        is InvalidDevId -> R.string.error_terra_invalid_dev_id
        is InvalidAuthToken -> R.string.error_terra_invalid_token
        is UserLimitExceeded -> R.string.error_terra_user_limit
        is NoInternet -> R.string.error_terra_no_internet
        is UnexpectedError -> R.string.error_terra_provider_not_enabled
        else -> R.string.error_health_connect_failed
    }

    private suspend fun persistHealthConnectConnection() {
        val terraUserId = terraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT)
            ?: return
        healthConnectionsRepository.upsertConnection(
            HealthConnection(
                terraUserId = terraUserId,
                provider = TerraProviders.HEALTH_CONNECT,
                source = HealthConnection.SOURCE_SDK,
                connectedAt = Timestamp.now(),
                active = true,
            ),
        ).onFailure { setError(R.string.error_connection_save_failed) }
    }

    private fun handleCallbackOutcome(outcome: TerraCallbackHandler.Outcome) {
        when (outcome) {
            is TerraCallbackHandler.Outcome.Success ->
                setMessage(R.string.msg_wearable_connected, outcome.provider.replaceFirstChar { it.titlecase() })
            is TerraCallbackHandler.Outcome.Failure ->
                setError(R.string.error_wearable_connect_failed, outcome.reason)
            is TerraCallbackHandler.Outcome.SaveFailed ->
                setError(R.string.error_connection_save_failed)
            TerraCallbackHandler.Outcome.Malformed ->
                setError(R.string.error_connection_callback_invalid)
            TerraCallbackHandler.Outcome.Ignored -> Unit
        }
    }
}
