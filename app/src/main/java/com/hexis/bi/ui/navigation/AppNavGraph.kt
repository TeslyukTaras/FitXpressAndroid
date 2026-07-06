package com.hexis.bi.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.SessionCleaner
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.ui.auth.forgotpassword.ForgotPasswordScreen
import com.hexis.bi.ui.auth.info.AppInfoScreen
import com.hexis.bi.ui.auth.login.LoginScreen
import com.hexis.bi.ui.auth.onboarding.OnboardingScreen
import com.hexis.bi.ui.auth.signup.SignUpScreen
import com.hexis.bi.ui.auth.verifyemail.VerifyEmailScreen
import com.hexis.bi.ui.main.MainScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val NAV_ANIM_DURATION_MS = 300

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val preferencesRepository: UserPreferencesRepository = koinInject()
    val authRepository: AuthRepository = koinInject()
    val sessionCleaner: SessionCleaner = koinInject()

    val startDestination by remember {
        combine(
            preferencesRepository.onboardingShown,
            authRepository.authState,
        ) { onboardingShown, isSignedIn ->
            when {
                !onboardingShown -> Route.APP_INFO
                isSignedIn && !authRepository.isEmailVerified -> Route.VERIFY_EMAIL
                isSignedIn -> Route.MAIN
                else -> Route.LOGIN
            }
        }.take(1)
    }.collectAsState(initial = null)

    val destination = startDestination ?: return

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var mainStartDestination by remember { mutableStateOf(Route.Main.HOME) }
    // PROFILE_ONBOARDING for a fresh sign-up, MAIN when resuming verification.
    var postVerifyDestination by remember { mutableStateOf(Route.MAIN) }

    NavHost(
        navController = navController,
        startDestination = destination,
        modifier = modifier,
        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(NAV_ANIM_DURATION_MS)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(NAV_ANIM_DURATION_MS)) },
        popEnterTransition = {
            slideIntoContainer(
                SlideDirection.Right,
                tween(NAV_ANIM_DURATION_MS)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                SlideDirection.Right,
                tween(NAV_ANIM_DURATION_MS)
            )
        },
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

        composable(Route.PROFILE_ONBOARDING) {
            OnboardingScreen(
                onBuySuitScanRequested = {
                    mainStartDestination = Route.Main.SUIT_SIZE_SCAN
                    navController.navigate(Route.MAIN) { launchSingleTop = true }
                },
                onFinish = {
                    mainStartDestination = Route.Main.HOME
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.PROFILE_ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Route.SIGN_UP) },
                onLoginSuccess = {
                    mainStartDestination = Route.Main.HOME
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Route.FORGOT_PASSWORD) },
                onNavigateToVerifyEmail = {
                    postVerifyDestination = Route.MAIN
                    navController.navigate(Route.VERIFY_EMAIL)
                },
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
                    mainStartDestination = Route.Main.HOME
                    navController.navigate(Route.PROFILE_ONBOARDING) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                // Keep Sign Up on the back stack so the user can return to it from verification.
                onNavigateToVerifyEmail = {
                    postVerifyDestination = Route.PROFILE_ONBOARDING
                    navController.navigate(Route.VERIFY_EMAIL)
                },
            )
        }
        composable(Route.VERIFY_EMAIL) {
            VerifyEmailScreen(
                onNavigateBack = {
                    // Leaving verification drops the unverified session and always returns to
                    // Login (the account still exists — signing in again resumes verification).
                    // Clear the whole stack so we land on a single fresh Login in every entry path.
                    scope.launch { sessionCleaner.signOut() }
                    navController.navigate(Route.LOGIN) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onVerified = {
                    navController.navigate(postVerifyDestination) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.MAIN) {
            MainScreen(
                startDestination = mainStartDestination,
                onExit = { navController.popBackStackOnce() },
                onLogout = {
                    scope.launch { sessionCleaner.signOut() }
                    mainStartDestination = Route.Main.HOME
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
