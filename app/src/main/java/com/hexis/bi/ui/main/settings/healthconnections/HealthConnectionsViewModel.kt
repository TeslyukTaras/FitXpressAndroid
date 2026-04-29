package com.hexis.bi.ui.main.settings.healthconnections

import android.app.Activity
import android.app.Application
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
    terraCallbackHandler: TerraCallbackHandler,
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
                _state.update {
                    it.copy(
                        connectedProviders = emptySet(),
                        wearableConnections = active,
                    )
                }
            }
            .catch { setError(R.string.error_connection_save_failed) }
            .launchIn(viewModelScope)

        terraCallbackHandler.outcomes
            .onEach(::handleCallbackOutcome)
            .launchIn(viewModelScope)
    }

    fun onSdkProviderRowClick(provider: String, displayName: String, activity: Activity?) {
        if (!provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true)) {
            onWidgetProviderRowClick(provider, displayName)
            return
        }
        if (activity == null) {
            setError(R.string.error_health_connect_failed)
            return
        }
        val sdkConnection = Connections.HEALTH_CONNECT
        val sdkConnected = terraManagerHolder.current?.getUserId(sdkConnection) != null
        val storedConnected = isWearableConnected(provider)
        if (sdkConnected || storedConnected) {
            disconnectSdkProvider(
                activity = activity,
                connection = sdkConnection,
                provider = provider,
                displayName = displayName,
            )
        } else {
            connectSdkProvider(
                activity = activity,
                connection = sdkConnection,
                provider = provider,
                displayName = displayName,
            )
        }
    }

    fun onOuraRowClick() {
        if (isWearableConnected(TerraProviders.OURA)) {
            disconnectWearableByProvider(
                TerraProviders.OURA,
                appContext.getString(R.string.health_connection_oura)
            )
        } else {
            onConnectOura()
        }
    }

    fun onDummyRowClick() {
        if (isWearableConnected(TerraProviders.DUMMY)) {
            disconnectWearableByProvider(
                TerraProviders.DUMMY,
                appContext.getString(R.string.health_connection_dummy)
            )
        } else {
            onConnectDummy()
        }
    }

    fun onWidgetProviderRowClick(provider: String, displayName: String) {
        if (isWearableConnected(provider)) {
            disconnectWearableByProvider(provider, displayName)
        } else {
            startWidgetSession(provider)
        }
    }

    fun onConnectOura() = startWidgetSession(TerraProviders.OURA)

    fun onConnectDummy() = startWidgetSession(TerraProviders.DUMMY)

    private fun isWearableConnected(terraProvider: String): Boolean =
        _state.value.wearableConnections.any {
            it.provider.equals(
                terraProvider,
                ignoreCase = true
            )
        }

    private fun disconnectWearableByProvider(terraProvider: String, providerName: String) =
        launch(
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
            setMessage(R.string.msg_wearable_disconnected, providerName)
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

    private fun disconnectSdkProvider(
        activity: Activity,
        connection: Connections,
        provider: String,
        displayName: String,
    ) = launch(
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
        val firestoreIds = connections
            .filter {
                it.active && it.provider.equals(
                    provider,
                    ignoreCase = true
                )
            }
            .map { it.terraUserId }
            .distinct()
        val sdkId = terraManagerHolder.current?.getUserId(connection)
        val allIds = (firestoreIds + listOfNotNull(sdkId)).distinct()
        if (allIds.isEmpty()) {
            setMessage(R.string.msg_health_connect_nothing_to_disconnect)
            return@launch
        }
        for (id in allIds) {
            terraApi.deauthenticateUser(id).onFailure { e ->
                Timber.w(e, "Terra deauthenticateUser failed user_id=%s", id)
            }
        }
        for (id in firestoreIds) {
            healthConnectionsRepository.deactivateConnection(id).onFailure {
                Timber.w(it, "deactivateConnection failed for %s", id)
            }
        }
        terraManagerHolder.clearLocalManager()
        terraManagerHolder.init(activity, uid)
            .onFailure { Timber.e(it, "Terra init after disconnect failed") }
        if (provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true)) {
            setMessage(R.string.msg_health_connect_disconnected)
        } else {
            setMessage(R.string.msg_wearable_disconnected, displayName)
        }
    }

    private fun connectSdkProvider(
        activity: Activity,
        connection: Connections,
        provider: String,
        displayName: String,
    ) = launch(
        onError = { setError(it.toSdkErrorRes()) },
    ) {
        val connected = terraConnector.connect(activity, connection).getOrElse {
            setError(it.toSdkErrorRes())
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
        persistSdkConnection(connection, provider)
        if (provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true)) {
            setMessage(R.string.msg_health_connect_connected)
        } else {
            setMessage(R.string.msg_wearable_connected, displayName)
        }
    }

    private fun Throwable.toSdkErrorRes(): Int = when (this) {
        is InvalidDevId -> R.string.error_terra_invalid_dev_id
        is InvalidAuthToken -> R.string.error_terra_invalid_token
        is UserLimitExceeded -> R.string.error_terra_user_limit
        is NoInternet -> R.string.error_terra_no_internet
        is UnexpectedError -> R.string.error_terra_provider_not_enabled
        else -> R.string.error_health_connect_failed
    }

    private suspend fun persistSdkConnection(connection: Connections, provider: String) {
        val terraUserId = terraManagerHolder.current?.getUserId(connection)
            ?: return
        healthConnectionsRepository.upsertConnection(
            HealthConnection(
                terraUserId = terraUserId,
                provider = provider,
                source = HealthConnection.SOURCE_SDK,
                connectedAt = Timestamp.now(),
                active = true,
            ),
        ).onFailure { setError(R.string.error_connection_save_failed) }
    }

    private fun handleCallbackOutcome(outcome: TerraCallbackHandler.Outcome) {
        when (outcome) {
            is TerraCallbackHandler.Outcome.Success ->
                setMessage(
                    R.string.msg_wearable_connected,
                    outcome.provider.replaceFirstChar { it.titlecase() })

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
