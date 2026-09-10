package com.example.haven.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.haven.data.repository.FamilyRepository
import com.example.haven.presentation.home.HomeScreen
import com.example.haven.presentation.onboarding.OnboardingScreen

object HavenDestinations {
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

@Composable
fun HavenNavHost(
    repository: FamilyRepository = FamilyRepository.instance
) {
    val navController = rememberNavController()
    val currentUser by repository.currentUser.collectAsState()
    val currentFamily by repository.currentFamily.collectAsState()

    val startDestination = when {
        currentUser == null -> HavenDestinations.AUTH
        currentFamily == null -> HavenDestinations.ONBOARDING
        else -> HavenDestinations.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) }
    ) {
        composable(HavenDestinations.AUTH) {
            com.example.haven.presentation.auth.AuthScreen(
                onAuthSuccess = {
                    val fam = repository.currentFamily.value
                    val nextDest = if (fam != null) HavenDestinations.HOME else HavenDestinations.ONBOARDING
                    navController.navigate(nextDest) {
                        popUpTo(HavenDestinations.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(HavenDestinations.ONBOARDING) {
            OnboardingScreen(
                onFamilyReady = {
                    navController.navigate(HavenDestinations.HOME) {
                        popUpTo(HavenDestinations.ONBOARDING) { inclusive = true }
                    }
                },
                onSignOut = {
                    navController.navigate(HavenDestinations.AUTH) {
                        popUpTo(HavenDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(HavenDestinations.HOME) {
            HomeScreen()
        }
    }
}
