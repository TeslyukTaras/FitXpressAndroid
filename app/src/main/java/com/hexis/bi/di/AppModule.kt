package com.hexis.bi.di

import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.FirebaseAuthRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordViewModel
import com.hexis.bi.ui.auth.login.LoginViewModel
import com.hexis.bi.ui.auth.signup.SignUpViewModel
import com.hexis.bi.ui.home.HomeViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { CredentialManager.create(androidContext()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get(), androidContext()) }
    single { UserPreferencesRepository(androidContext()) }
    viewModel { LoginViewModel(get(), androidApplication()) }
    viewModel { SignUpViewModel(get(), androidApplication()) }
    viewModel { HomeViewModel(get(), androidApplication()) }
    viewModel { ForgotPasswordViewModel(get(), androidApplication()) }
}
