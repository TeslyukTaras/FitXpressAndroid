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
import com.hexis.bi.ui.auth.info.AppInfoScreen
import com.hexis.bi.ui.auth.login.LoginScreen
import com.hexis.bi.ui.auth.signup.SignUpScreen
import com.hexis.bi.ui.main.MainScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.compose.koinInject



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
                isSignedIn -> Route.MAIN
                else -> Route.LOGIN
            }
        }.take(1)
    }.collectAsState(initial = null)

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
                    navController.navigate(Route.MAIN) {
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
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.MAIN) {
            MainScreen(
                onLogout = {
                    scope.launch { authRepository.signOut() }
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.MAIN) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
