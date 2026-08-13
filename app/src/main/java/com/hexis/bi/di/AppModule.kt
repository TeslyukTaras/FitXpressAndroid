package com.hexis.bi.di

import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.activity.DefaultActivityRepository
import com.hexis.bi.data.health.local.HealthAggregateDatabase
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.local.CanonicalUserCacheCleaner
import com.hexis.bi.BuildConfig
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.EmailVerificationApi
import com.hexis.bi.data.auth.FirebaseAuthRepository
import com.hexis.bi.data.auth.SessionCleaner
import com.hexis.bi.data.healthconnect.HealthConnectPermissionChecker
import com.hexis.bi.data.healthconnections.FirestoreHealthConnectionsRepository
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.data.network.httpLoggingInterceptor
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.order.FirestoreOrderRepository
import com.hexis.bi.data.order.OrderDraftHolder
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.recovery.DefaultRecoveryRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.reminder.ScanReminderSchedulerImpl
import com.hexis.bi.data.reminder.ScanReminderWorkRunner
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.data.intelligence.AssetIntelligenceConfigSource
import com.hexis.bi.data.intelligence.RemoteIntelligenceConfigSource
import com.hexis.bi.utils.constants.IntelligenceRemoteConfig
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.data.intelligence.BUNDLED_WORDING_ASSET
import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.data.intelligence.IntelligenceWordingRepository
import com.hexis.bi.data.intelligence.IntelligenceInputProvider
import com.hexis.bi.data.health.sync.HealthSyncCoordinator
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.health.sync.WorkManagerHealthSyncScheduler
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.scan.api.ThreeDLookApi
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.DefaultSleepRepository
import com.hexis.bi.data.store.AppPreferencesDataStore
import com.hexis.bi.data.suit.MockSuitRepository
import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraAuthApi
import com.hexis.bi.data.terra.TerraCallbackHandler
import com.hexis.bi.data.terra.TerraConnectionReconciler
import com.hexis.bi.data.terra.TerraConnector
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.TerraSdkConnectionOwnership
import com.hexis.bi.data.terra.TerraWidgetApi
import com.hexis.bi.data.user.FirestoreUserRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.MainViewModel
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordViewModel
import com.hexis.bi.ui.auth.login.LoginViewModel
import com.hexis.bi.ui.auth.onboarding.OnboardingViewModel
import com.hexis.bi.ui.auth.signup.SignUpViewModel
import com.hexis.bi.ui.auth.verifyemail.VerifyEmailViewModel
import com.hexis.bi.ui.main.body.BodyViewModel
import com.hexis.bi.ui.main.buysuit.editaddress.EditAddressViewModel
import com.hexis.bi.ui.main.buysuit.shipping.ShippingDetailsViewModel
import com.hexis.bi.ui.main.buysuit.suitsize.SuitSizeResultsViewModel
import com.hexis.bi.ui.main.home.HomeViewModel
import com.hexis.bi.ui.main.home.activity.ActivityViewModel
import com.hexis.bi.ui.main.home.longevity.LongevityViewModel
import com.hexis.bi.ui.main.home.intelligence.InsightsViewModel
import com.hexis.bi.ui.main.home.paceofaging.PaceOfAgingViewModel
import com.hexis.bi.ui.main.home.physiquedrift.PhysiqueDriftViewModel
import com.hexis.bi.ui.main.home.recomposition.RecompositionViewModel
import com.hexis.bi.ui.main.home.recovery.RecoveryViewModel
import com.hexis.bi.ui.main.home.sleep.SleepViewModel
import com.hexis.bi.ui.main.notifications.NotificationsViewModel
import com.hexis.bi.ui.main.scan.ScanViewModel
import com.hexis.bi.ui.main.scan.history.ScanHistoryViewModel
import com.hexis.bi.ui.main.scan.results.ResultsViewModel
import com.hexis.bi.ui.main.scan.startscan.StartScanViewModel
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountViewModel
import com.hexis.bi.ui.main.settings.editprofile.EditProfileViewModel
import com.hexis.bi.ui.main.settings.healthconnections.HealthConnectionsViewModel
import com.hexis.bi.ui.main.settings.mysuit.MySuitViewModel
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsViewModel
import com.hexis.bi.ui.main.settings.scanpreferences.ScanPreferencesViewModel
import com.hexis.bi.utils.constants.NetworkConstants.HTTP_TIMEOUT_SECONDS
import com.hexis.bi.utils.permissions.NotificationPermissionCoordinator
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseFunctions.getInstance(FIREBASE_FUNCTIONS_REGION) }
    single { FirebaseStorage.getInstance() }
    single { CredentialManager.create(androidContext()) }
    single { AppPreferencesDataStore(androidContext()) }
    single { HealthAggregateDatabase(androidContext()) }
    single { HealthLocalDataSource(get(), BuildConfig.ENVIRONMENT) }
    single<CanonicalUserCacheCleaner> { get<HealthLocalDataSource>() }
    single { UserPreferencesRepository(get()) }
    single { NotificationInboxRepository(androidContext(), get()) }
    single {
        ScanReminderWorkRunner(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single<ScanReminderScheduler> { ScanReminderSchedulerImpl(androidContext(), get(), get()) }
    single { NotificationPermissionCoordinator(androidContext(), get(), get(), get(), get()) }
    single { EmailVerificationApi(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get(), get(), androidContext()) }
    single { HealthConnectPermissionChecker(androidContext()) }
    single { HealthRemoteDataSource(get(), get()) }
    single { HealthSyncCoordinator(get(), get(), get(), get(), get(), get(), get()) }
    single { FirebaseRemoteConfig.getInstance() }
    single {
        IntelligenceConfigRepository(
            overrides = listOf(
                RemoteIntelligenceConfigSource(
                    remoteConfig = get(),
                    key = IntelligenceRemoteConfig.CONFIG_KEY,
                    minimumFetchIntervalSeconds = intelligenceFetchInterval(),
                ),
            ),
            baseline = AssetIntelligenceConfigSource(androidContext()),
        )
    }
    single {
        IntelligenceWordingRepository(
            overrides = listOf(
                RemoteIntelligenceConfigSource(
                    remoteConfig = get(),
                    key = IntelligenceRemoteConfig.WORDING_KEY,
                    minimumFetchIntervalSeconds = intelligenceFetchInterval(),
                ),
            ),
            baseline = AssetIntelligenceConfigSource(androidContext(), BUNDLED_WORDING_ASSET),
        )
    }
    single { IntelligenceInputProvider(get(), get(), get()) }
    single { RunIntelligenceUseCase(get(), get(), get(), get()) }
    single { WorkManager.getInstance(androidContext()) }
    single<HealthSyncScheduler> { WorkManagerHealthSyncScheduler(get()) }
    single { SessionCleaner(get(), get(), get(), get(), get(), get(), get()) }
    single<SuitRepository> { MockSuitRepository(get()) }
    single<UserRepository> { FirestoreUserRepository(get(), get(), androidContext()) }
    single {
        OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply { httpLoggingInterceptor()?.let { addInterceptor(it) } }
            .build()
    }
    single { ThreeDLookApi(get(), get(), get(), androidContext()) }
    single { ThreeDLookRepository(get(), get()) }
    single { ScanResultRepository() }
    single { ScanHistoryRepository(get(), get(), get()) }
    single { OrderDraftHolder() }
    single<OrderRepository> { FirestoreOrderRepository(get(), get()) }
    single { TerraAuthApi(get()) }
    single { TerraApi(get()) }
    single<HealthConnectionsRepository> {
        FirestoreHealthConnectionsRepository(
            get(),
            get(),
            androidContext()
        )
    }
    single { TerraManagerHolder() }
    single { TerraRestSourceResolver(get(), get()) }
    single { TerraSdkConnectionOwnership(get(), get(), get()) }
    single { TerraConnectionReconciler(get(), get()) }
    single { TerraCallbackHandler(get()) }
    single { TerraWidgetApi(get()) }
    single { TerraConnector(get(), get()) }
    single<SleepRepository> {
        DefaultSleepRepository(api = get(), remote = get(), local = get(), auth = get())
    }
    single<ActivityRepository> {
        DefaultActivityRepository(api = get(), remote = get(), local = get(), auth = get())
    }
    single<RecoveryRepository> {
        DefaultRecoveryRepository(sleepRepository = get(), activityRepository = get())
    }
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get(), androidApplication()) }
    viewModel { SignUpViewModel(get(), get(), get(), androidApplication()) }
    viewModel {
        HomeViewModel(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { EditProfileViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { ForgotPasswordViewModel(get(), androidApplication()) }
    viewModel { VerifyEmailViewModel(get(), androidApplication()) }
    viewModel { ScanPreferencesViewModel(androidApplication(), get(), get()) }
    viewModel {
        HealthConnectionsViewModel(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { MySuitViewModel(androidApplication(), get(), get()) }
    viewModel { NotificationsSettingsViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { NotificationsViewModel(androidApplication(), get()) }
    viewModel { BodyViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { SleepViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ActivityViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { RecoveryViewModel(androidApplication(), get()) }
    viewModel { LongevityViewModel(androidApplication(), get(), get(), get(), get(), get(), get()) }
    viewModel { PaceOfAgingViewModel(androidApplication(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { PhysiqueDriftViewModel(androidApplication(), get(), get(), get()) }
    viewModel { InsightsViewModel(androidApplication(), get(), get()) }
    viewModel { RecompositionViewModel(androidApplication(), get()) }
    viewModel { ScanViewModel(androidApplication(), get()) }
    viewModel { StartScanViewModel(androidApplication(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ResultsViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { SuitSizeResultsViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { ShippingDetailsViewModel(androidApplication(), get(), get(), get()) }
    viewModel { (orderId: String) -> EditAddressViewModel(androidApplication(), get(), orderId) }
    viewModel { ScanHistoryViewModel(androidApplication(), get(), get()) }
    viewModel { DeleteAccountViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(androidApplication(), get(), get()) }
}

private const val FIREBASE_FUNCTIONS_REGION = "us-central1"

private fun intelligenceFetchInterval(): Long = if (BuildConfig.DEBUG) {
    IntelligenceRemoteConfig.DEBUG_FETCH_INTERVAL_SECONDS
} else {
    IntelligenceRemoteConfig.RELEASE_FETCH_INTERVAL_SECONDS
}
