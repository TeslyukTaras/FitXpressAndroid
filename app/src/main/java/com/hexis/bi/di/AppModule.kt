package com.hexis.bi.di

import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.FirebaseAuthRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.scan.api.ThreeDLookApi
import com.hexis.bi.data.suit.MockSuitRepository
import com.hexis.bi.data.user.FirestoreUserRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.MainViewModel
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordViewModel
import com.hexis.bi.ui.auth.login.LoginViewModel
import com.hexis.bi.ui.auth.onboarding.OnboardingViewModel
import com.hexis.bi.ui.auth.signup.SignUpViewModel
import com.hexis.bi.ui.main.home.HomeViewModel
import com.hexis.bi.ui.main.home.recovery.RecoveryViewModel
import com.hexis.bi.ui.main.home.sleep.SleepViewModel
import com.hexis.bi.ui.main.scan.ScanViewModel
import com.hexis.bi.ui.main.scan.results.ResultsViewModel
import com.hexis.bi.ui.main.scan.startscan.StartScanViewModel
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountViewModel
import com.hexis.bi.ui.main.settings.editprofile.EditProfileViewModel
import com.hexis.bi.ui.main.settings.healthconnections.HealthConnectionsViewModel
import com.hexis.bi.ui.main.settings.mysuit.MySuitViewModel
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsViewModel
import com.hexis.bi.ui.main.settings.scanpreferences.ScanPreferencesViewModel
import com.hexis.bi.utils.constants.NetworkConstants.HTTP_TIMEOUT_SECONDS
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { CredentialManager.create(androidContext()) }
    single { UserPreferencesRepository(androidContext()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get(), androidContext()) }
    single<SuitRepository> { MockSuitRepository(get()) }
    single<UserRepository> { FirestoreUserRepository(get(), get(), androidContext()) }
    single {
        OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    single { ThreeDLookApi(get(), androidContext()) }
    single { ThreeDLookRepository(get()) }
    single { ScanResultRepository() }
    single { ScanHistoryRepository(get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get(), androidApplication()) }
    viewModel { SignUpViewModel(get(), get(), get(), androidApplication()) }
    viewModel { HomeViewModel(androidApplication(), get(), get()) }
    viewModel { EditProfileViewModel(androidApplication(), get(), get(), get()) }
    viewModel { ForgotPasswordViewModel(get(), androidApplication()) }
    viewModel { ScanPreferencesViewModel(androidApplication(), get()) }
    viewModel { HealthConnectionsViewModel(androidApplication()) }
    viewModel { MySuitViewModel(androidApplication(), get(), get()) }
    viewModel { NotificationsSettingsViewModel(androidApplication()) }
    viewModel { SleepViewModel(androidApplication()) }
    viewModel { RecoveryViewModel(androidApplication()) }
    viewModel { ScanViewModel(androidApplication(), get()) }
    viewModel { StartScanViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { ResultsViewModel(androidApplication(), get(), get(), get()) }
    viewModel { DeleteAccountViewModel(androidApplication(), get(), get(), get()) }
    viewModel { OnboardingViewModel(androidApplication(), get(), get()) }
}
