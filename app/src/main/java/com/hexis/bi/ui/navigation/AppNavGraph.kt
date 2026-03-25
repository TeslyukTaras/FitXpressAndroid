package com.hexis.bi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.ui.auth.login.LoginScreen
import com.hexis.bi.ui.auth.signup.SignUpScreen
import com.hexis.bi.ui.info.AppInfoScreen

private object Route {
    const val APP_INFO = "app_info"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.APP_INFO,
        modifier = modifier,
    ) {
        composable(Route.APP_INFO) {
            AppInfoScreen(
                onFinish = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.APP_INFO) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Route.SIGN_UP) },
                onLoginSuccess = { /* TODO: navigate to home */ },
            )
        }
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = { /* TODO: navigate to home */ },
            )
        }
    }
}
