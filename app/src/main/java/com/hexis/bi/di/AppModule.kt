package com.hexis.bi.di

import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.FirebaseAuthRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.user.FirestoreUserRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.MainViewModel
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordViewModel
import com.hexis.bi.ui.auth.login.LoginViewModel
import com.hexis.bi.ui.auth.signup.SignUpViewModel
import com.hexis.bi.ui.main.home.HomeViewModel
import com.hexis.bi.ui.main.settings.editprofile.EditProfileViewModel
import com.hexis.bi.ui.main.settings.healthconnections.HealthConnectionsViewModel
import com.hexis.bi.ui.main.settings.mysuit.MySuitViewModel
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountViewModel
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsViewModel
import com.hexis.bi.ui.main.settings.scanpreferences.ScanPreferencesViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { CredentialManager.create(androidContext()) }
    single { UserPreferencesRepository(androidContext()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get(), androidContext()) }
    single<UserRepository> { FirestoreUserRepository(get(), get(), androidContext()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get(), androidApplication()) }
    viewModel { SignUpViewModel(get(), get(), get(), androidApplication()) }
    viewModel { HomeViewModel(androidApplication(), get()) }
    viewModel { EditProfileViewModel(androidApplication(), get(), get(), get()) }
    viewModel { ForgotPasswordViewModel(get(), androidApplication()) }
    viewModel { ScanPreferencesViewModel(androidApplication(), get()) }
    viewModel { HealthConnectionsViewModel(androidApplication()) }
    viewModel { MySuitViewModel(androidApplication()) }
    viewModel { NotificationsSettingsViewModel(androidApplication()) }
    viewModel { DeleteAccountViewModel(androidApplication(), get(), get(), get()) }
}
