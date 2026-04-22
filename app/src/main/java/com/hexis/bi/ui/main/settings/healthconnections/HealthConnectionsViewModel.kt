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
import com.hexis.bi.data.terra.TerraCallbackHandler
import com.hexis.bi.data.terra.TerraConnector
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraWidgetApi
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class HealthConnectionsViewModel(
    application: Application,
    private val terraConnector: TerraConnector,
    private val terraWidgetApi: TerraWidgetApi,
    private val healthConnectionsRepository: HealthConnectionsRepository,
    private val terraCallbackHandler: TerraCallbackHandler,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HealthConnectionsState())
    val state = _state.asStateFlow()

    init {
        refreshSdkConnections()

        healthConnectionsRepository.observeConnections()
            .onEach { items ->
                _state.update { it.copy(wearableConnections = items.filter { c -> c.active }) }
            }
            .catch { setError(R.string.error_connection_save_failed) }
            .launchIn(viewModelScope)

        terraCallbackHandler.outcomes
            .onEach(::handleCallbackOutcome)
            .launchIn(viewModelScope)
    }

    fun onProviderClick(provider: HealthProvider, activity: Activity) {
        when (provider) {
            HealthProvider.GoogleHealth -> connectHealthConnect(activity)
            HealthProvider.AppleHealth -> Unit
        }
    }

    fun onConnectWearable() = launch(
        onError = { setError(R.string.error_wearable_start_failed) },
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            setError(R.string.error_sign_in_required)
            return@launch
        }

        terraWidgetApi.generateWidgetSession(referenceId = uid)
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

    private fun connectHealthConnect(activity: Activity) = launch(
        onError = { setError(it.toHealthConnectErrorRes()) },
    ) {
        terraConnector.connect(activity, Connections.HEALTH_CONNECT)
            .onSuccess {
                refreshSdkConnections()
                persistHealthConnectConnection()
                setMessage(R.string.msg_health_connect_connected)
            }
            .onFailure { setError(it.toHealthConnectErrorRes()) }
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
        val terraUserId = TerraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT)
            ?: return
        healthConnectionsRepository.upsertConnection(
            HealthConnection(
                terraUserId = terraUserId,
                provider = PROVIDER_HEALTH_CONNECT,
                source = HealthConnection.SOURCE_SDK,
                connectedAt = Timestamp.now(),
                active = true,
            ),
        ).onFailure { setError(R.string.error_connection_save_failed) }
    }

    private fun refreshSdkConnections() {
        val connected = buildSet {
            if (TerraManagerHolder.isConnected(Connections.HEALTH_CONNECT)) {
                add(HealthProvider.GoogleHealth)
            }
        }
        _state.update { it.copy(connectedProviders = connected) }
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

    companion object {
        private const val PROVIDER_HEALTH_CONNECT = "HEALTH_CONNECT"
    }
}
