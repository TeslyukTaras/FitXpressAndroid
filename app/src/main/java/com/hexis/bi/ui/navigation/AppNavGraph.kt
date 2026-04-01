package com.hexis.bi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordScreen
import com.hexis.bi.ui.auth.login.LoginScreen
import com.hexis.bi.ui.auth.signup.SignUpScreen
import com.hexis.bi.ui.home.HomeScreen
import com.hexis.bi.ui.info.AppInfoScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private object Route {
    const val APP_INFO = "app_info"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val FORGOT_PASSWORD = "forgot_password"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val preferencesRepository: UserPreferencesRepository = koinInject()
    val authRepository: AuthRepository = koinInject()

    val startDestination by remember {
        combine(
            preferencesRepository.onboardingShown,
            authRepository.authState,
        ) { onboardingShown, isSignedIn ->
            when {
                !onboardingShown -> Route.APP_INFO
                isSignedIn -> Route.HOME
                else -> Route.LOGIN
            }
        }
    }.collectAsState(initial = null)

    // Wait until both DataStore and Firebase have emitted their initial values
    val destination = startDestination ?: return

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = destination,
        modifier = modifier,
    ) {
        composable(Route.APP_INFO) {
            AppInfoScreen(
                onFinish = {
                    scope.launch { preferencesRepository.setOnboardingShown() }
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.APP_INFO) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Route.SIGN_UP) },
                onLoginSuccess = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Route.FORGOT_PASSWORD) },
            )
        }
        composable(Route.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
