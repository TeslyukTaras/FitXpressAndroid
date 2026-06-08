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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber

/** Set false to show all Terra rows again (including those still on [R.drawable.ic_connect]). */
private const val HIDE_PROVIDERS_WITHOUT_BRANDED_ICON = true

private const val VERIFY_MAX_ATTEMPTS = 5
private const val VERIFY_RETRY_DELAY_MS = 2_000L

class HealthConnectionsViewModel(
    application: Application,
    private val terraConnector: TerraConnector,
    private val terraWidgetApi: TerraWidgetApi,
    private val healthConnectionsRepository: HealthConnectionsRepository,
    private val terraCallbackHandler: TerraCallbackHandler,
    private val firebaseAuth: FirebaseAuth,
    private val terraApi: TerraApi,
    private val terraManagerHolder: TerraManagerHolder,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(HealthConnectionsState())
    val state = _state.asStateFlow()

    private var verifyJob: Job? = null

    private fun buildSdkProviders(): List<TerraProviderUi> = listOf(
        TerraProviderUi(
            code = TerraProviders.HEALTH_CONNECT,
            label = string(R.string.provider_health_connect),
            iconRes = R.drawable.ic_google,
        ),
    )

    private fun provider(
        code: String,
        @androidx.annotation.StringRes labelRes: Int,
        @androidx.annotation.DrawableRes iconRes: Int = R.drawable.ic_connect,
    ): TerraProviderUi =
        TerraProviderUi(code = code, label = string(labelRes), iconRes = iconRes)

    private fun buildWearableProviders(): List<TerraProviderUi> = listOf(
        provider(TerraProviders.AKTIIA, R.string.provider_aktiia, R.drawable.img_aktiia),
        provider(TerraProviders.BIOSTRAP, R.string.provider_biostrap, R.drawable.img_biostrap),
        provider(TerraProviders.CARDIOMOOD, R.string.provider_cardiomood, R.drawable.img_cardiomood),
        provider(TerraProviders.DEXCOM, R.string.provider_dexcom, R.drawable.img_dexcom),
        provider(TerraProviders.DEXCOM_EU, R.string.provider_dexcomeu, R.drawable.img_dexcom),
        provider(TerraProviders.FITBIT, R.string.provider_fitbit, R.drawable.img_fitbit),
        provider(TerraProviders.GARMIN, R.string.provider_garmin, R.drawable.img_garmin),
        provider(TerraProviders.HEALTHGAUGE, R.string.provider_healthgauge, R.drawable.img_healthgauge),
        provider(TerraProviders.HUAWEI, R.string.provider_huawei, R.drawable.img_huawei),
        provider(TerraProviders.INBODY, R.string.provider_inbody),
        provider(TerraProviders.MOXY, R.string.provider_moxy, R.drawable.img_moxy),
        provider(TerraProviders.OMRON, R.string.provider_omroneu),
        provider(TerraProviders.OMRONUS, R.string.provider_omronus),
        provider(TerraProviders.OURA, R.string.provider_oura, R.drawable.img_oura),
        provider(TerraProviders.POLAR, R.string.provider_polar, R.drawable.img_polar),
        provider(TerraProviders.PUL, R.string.provider_pul, R.drawable.img_pul),
        provider(TerraProviders.SOMNOFY, R.string.provider_somnofy, R.drawable.img_somnofy),
        provider(TerraProviders.SUUNTO, R.string.provider_suunto),
        provider(TerraProviders.WHOOP, R.string.provider_whoop, R.drawable.img_whoop),
        provider(TerraProviders.WITHINGS, R.string.provider_withings, R.drawable.img_withings),
        provider(TerraProviders.ZEPP, R.string.provider_zepp, R.drawable.img_zepp),
    )

    private fun buildOtherProviders(): List<TerraProviderUi> = listOf(
        provider(TerraProviders.BODITRAX, R.string.provider_boditrax),
        provider(TerraProviders.BRYTONSPORT, R.string.provider_brytonsport, R.drawable.img_bryton_sport),
        provider(TerraProviders.CATAPULTONE, R.string.provider_catapultone),
        provider(TerraProviders.CLUE, R.string.provider_clue, R.drawable.img_clue),
        provider(TerraProviders.CONCEPT2, R.string.provider_concept2, R.drawable.img_concept2),
        provider(TerraProviders.CORE, R.string.provider_core, R.drawable.img_core),
        provider(TerraProviders.CRONOMETER, R.string.provider_cronometer, R.drawable.img_cronometer),
        provider(TerraProviders.CYCLINGANALYTICS, R.string.provider_cyclinganalytics, R.drawable.img_cyclinganalytics),
        provider(TerraProviders.DECATHLON, R.string.provider_decathlon),
        provider(TerraProviders.EATTHISMUCH, R.string.provider_eatthismuch, R.drawable.img_eat_this_much),
        provider(TerraProviders.ELITEHRV, R.string.provider_elitehrv, R.drawable.img_elite_hrv),
        provider(TerraProviders.FATSECRET, R.string.provider_fatsecret),
        provider(TerraProviders.FINALSURGE, R.string.provider_finalsurge, R.drawable.img_final_surge),
        provider(TerraProviders.FLO, R.string.provider_flo, R.drawable.img_flo),
        provider(TerraProviders.GOOGLE, R.string.provider_google, R.drawable.img_google_fit),
        provider(TerraProviders.HAMMERHEAD, R.string.provider_hammerhead, R.drawable.img_hammerhead),
        provider(TerraProviders.HEVY, R.string.provider_hevy),
        provider(TerraProviders.KETOMOJOEU, R.string.provider_ketomojoeu),
        provider(TerraProviders.KETOMOJOUS, R.string.provider_ketomojous),
        provider(TerraProviders.KOMOOT, R.string.provider_komoot, R.drawable.img_komoot),
        provider(TerraProviders.LEZYNE, R.string.provider_lezyne, R.drawable.img_lezyne),
        provider(TerraProviders.MACROSFIRST, R.string.provider_macrosfirst),
        provider(TerraProviders.MAPMYFITNESS, R.string.provider_mapmyfitness, R.drawable.img_mapmyfitness),
        provider(TerraProviders.MAPMYTRACKS, R.string.provider_mapmytracks),
        provider(TerraProviders.MYMACROSPLUS, R.string.provider_mymacrosplus, R.drawable.img_mymacrosplus),
        provider(TerraProviders.MYFITNESSPAL, R.string.provider_myfitnesspal),
        provider(TerraProviders.NOLIO, R.string.provider_nolio),
        provider(TerraProviders.NUTRACHECK, R.string.provider_nutracheck),
        provider(TerraProviders.RIDEWITHGPS, R.string.provider_ridewithgps, R.drawable.img_ride_with_gps),
        provider(TerraProviders.ROUVY, R.string.provider_rouvy, R.drawable.img_rouvy),
        provider(TerraProviders.STRAVA, R.string.provider_strava, R.drawable.img_strava),
        provider(TerraProviders.TECHNOGYM, R.string.provider_technogym, R.drawable.img_technogym),
        provider(TerraProviders.TRAINERROAD, R.string.provider_trainerroad, R.drawable.img_trainer_road),
        provider(TerraProviders.TRAINASONE, R.string.provider_trainasone, R.drawable.img_trainasone),
        provider(TerraProviders.TRAINXHALE, R.string.provider_trainxhale, R.drawable.img_trainxhale),
        provider(TerraProviders.TRAININGPEAKS, R.string.provider_trainingpeaks),
        provider(TerraProviders.TREDICT, R.string.provider_tredict, R.drawable.img_tredict),
        provider(TerraProviders.TRIDOT, R.string.provider_tridot, R.drawable.img_tridot),
        provider(TerraProviders.ULTRAHUMAN, R.string.provider_ultrahuman, R.drawable.img_ultrahuman),
        provider(TerraProviders.UNDERARMOUR, R.string.provider_underarmour, R.drawable.img_under_armour),
        provider(TerraProviders.VELOHERO, R.string.provider_velohero, R.drawable.img_velohero),
        provider(TerraProviders.VIRTUAGYM, R.string.provider_virtuagym),
        provider(TerraProviders.WGER, R.string.provider_wger),
        provider(TerraProviders.XERT, R.string.provider_xert, R.drawable.img_xert),
        provider(TerraProviders.XOSS, R.string.provider_xoss, R.drawable.img_xoss),
        provider(TerraProviders.ZWIFT, R.string.provider_zwift, R.drawable.img_zwift),
    )

    /**
     * While [HIDE_PROVIDERS_WITHOUT_BRANDED_ICON] is true, drops rows that still use the generic
     * connect icon — except when the user already has an active Terra connection so they can disconnect.
     */
    private fun List<TerraProviderUi>.filterVisibleForDisplay(
        activeConnections: List<HealthConnection>,
    ): List<TerraProviderUi> {
        if (!HIDE_PROVIDERS_WITHOUT_BRANDED_ICON) return this
        return filter { ui ->
            ui.iconRes != R.drawable.ic_connect ||
                activeConnections.any { c ->
                    c.active && TerraProviders.storedMatchesUi(c.provider, ui.code)
                }
        }
    }

    override fun onInitialize() {
        _state.update {
            it.copy(
                sdkProviders = buildSdkProviders(),
                wearableProviders = buildWearableProviders(),
                otherProviders = buildOtherProviders(),
            )
        }

        healthConnectionsRepository.observeConnections()
            .onEach { items ->
                val active = items.filter { it.active }
                _state.update {
                    it.copy(
                        connectedProviders = emptySet(),
                        wearableConnections = active,
                        wearableProviders = buildWearableProviders().filterVisibleForDisplay(active),
                        otherProviders = buildOtherProviders().filterVisibleForDisplay(active),
                    )
                }
            }
            .catch { setError(R.string.error_connection_save_failed) }
            .launchIn(viewModelScope)

        terraCallbackHandler.outcomes
            .onEach(::handleCallbackOutcome)
            .launchIn(viewModelScope)

        setLoading(false)
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

    fun onDummyRowClick() {
        if (isWearableConnected(TerraProviders.DUMMY)) {
            disconnectWearableByProvider(
                TerraProviders.DUMMY,
                string(R.string.health_connection_dummy)
            )
        } else {
            onConnectDummy()
        }
    }

    fun onWidgetProviderRowClick(provider: String, displayName: String) {
        if (isWearableConnected(provider)) {
            disconnectWearableByProvider(provider, displayName)
        } else {
            startProviderAuth(provider)
        }
    }

    /** Dummy keeps the widget flow — it has no real OAuth. */
    fun onConnectDummy() = startWidgetSession(TerraProviders.DUMMY)

    private fun isWearableConnected(terraProvider: String): Boolean =
        _state.value.wearableConnections.any {
            TerraProviders.storedMatchesUi(it.provider, terraProvider)
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
                .filter { it.active && TerraProviders.storedMatchesUi(it.provider, terraProvider) }
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
            TerraSdkSync.invalidateCachesAndNotify()
            setMessage(R.string.msg_wearable_disconnected, providerName)
        }

    /**
     * Direct per-provider OAuth: opens the provider's auth page with no Terra widget landing.
     * Use [startWidgetSession] only for providers without a real OAuth (e.g. DUMMY).
     */
    private fun startProviderAuth(resource: String) = launch(
        onError = { setError(R.string.error_wearable_start_failed) },
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            setError(R.string.error_sign_in_required)
            return@launch
        }

        terraWidgetApi.authenticateUser(referenceId = uid, resource = resource)
            .onSuccess { session ->
                _state.update {
                    it.copy(
                        widgetUrl = session.authUrl,
                        pendingAuthUserId = session.userId,
                        pendingAuthProvider = resource,
                    )
                }
            }
            .onFailure { setError(R.string.error_wearable_start_failed) }
    }

    /**
     * On return from the OAuth Custom Tab, confirm the pending connection with Terra and persist
     * it — the redirect can't be relied on to reopen the app. Polls a few times since Terra may
     * lag a moment behind the completed OAuth.
     */
    fun onScreenResumed() {
        val userId = _state.value.pendingAuthUserId ?: return
        val provider = _state.value.pendingAuthProvider ?: return
        if (verifyJob?.isActive == true) return
        verifyJob = launch(showLoading = false, onError = { clearPendingAuth() }) {
            repeat(VERIFY_MAX_ATTEMPTS) {
                val info = terraApi.getUserInfo(userId).getOrNull()
                if (info?.isConnected == true) {
                    persistVerifiedConnection(userId, info.user?.provider ?: provider)
                    clearPendingAuth()
                    return@launch
                }
                delay(VERIFY_RETRY_DELAY_MS)
            }
            // No connection after retries — the user likely cancelled; drop it silently.
            clearPendingAuth()
        }
    }

    private suspend fun persistVerifiedConnection(userId: String, provider: String) {
        healthConnectionsRepository.upsertConnection(
            HealthConnection(
                terraUserId = userId,
                provider = provider.uppercase(),
                source = HealthConnection.SOURCE_API,
                connectedAt = Timestamp.now(),
                active = true,
            ),
        ).onSuccess {
            TerraSdkSync.invalidateCachesAndNotify()
            setMessage(R.string.msg_wearable_connected, provider.replaceFirstChar { it.titlecase() })
        }.onFailure {
            setError(R.string.error_connection_save_failed)
        }
    }

    private fun clearPendingAuth() {
        _state.update { it.copy(pendingAuthUserId = null, pendingAuthProvider = null) }
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
                it.active && TerraProviders.storedMatchesUi(it.provider, provider)
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
        TerraSdkSync.invalidateCachesAndNotify()
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
