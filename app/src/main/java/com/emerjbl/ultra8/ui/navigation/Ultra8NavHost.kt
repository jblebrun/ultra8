package com.emerjbl.ultra8.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.emerjbl.ultra8.ui.catalog.CatalogScreen
import com.emerjbl.ultra8.ui.gameplay.PlayScreen
import com.emerjbl.ultra8.ui.loading.InitialLoadScreen
import com.emerjbl.ultra8.ui.loading.LoadScreen
import com.emerjbl.ultra8.ui.settings.ProgramSettingsScreen
import kotlinx.coroutines.flow.Flow


@Composable
fun Ultra8NavHost(
    navController: NavHostController,
    gameShouldRun: Boolean,
    resetEvents: Flow<Unit>,
    onDrawerOpen: () -> Unit,
    onActiveProgram: (String) -> Unit,
) {
    NavHost(
        navController,
        startDestination = InitialLoad,
    ) {
        composable<InitialLoad> {
            InitialLoadScreen(
                onDrawerOpen,
                onCatalog = {
                    navController.navigate(Catalog)
                },
            )
        }
        composable<Catalog> {
            CatalogScreen(
                onSelectProgram = {
                    navController.navigate(PlayGame(it)) {
                        // https://issuetracker.google.com/issues/370694831
                        @SuppressLint("RestrictedApi")
                        popUpTo(Catalog) {
                            inclusive = true
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<ProgramSettings>(
            enterTransition = { fullSlideInFromRight },
            popEnterTransition = { fullSlideInFromRight },
            exitTransition = { fullSlideOutToRight },
            popExitTransition = { fullSlideOutToRight }
        ) { entry ->
            val routeProgram = entry.toRoute<PlayGame>().programName
            ProgramSettingsScreen(
                routeProgram,
                closeDialog = {
                    navController.popBackStack()
                }
            )
        }
        composable<PlayGame>(
            enterTransition = {
                if (initialState.destination.hasRoute<ProgramSettings>()) {
                    fullSlideInFromLeft
                } else {
                    fadeIn
                }
            },
            exitTransition = {
                if (targetState.destination.hasRoute<ProgramSettings>()) {
                    fullSlideOutToLeft
                } else {
                    fadeOut
                }
            }
        ) { entry ->
            val routeProgram = entry.toRoute<PlayGame>().programName
            // Prevent game running when view is animating away.
            val gameShouldRun =
                gameShouldRun && entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            onActiveProgram(routeProgram)
            PlayScreen(
                programName = routeProgram,
                gameShouldRun = gameShouldRun,
                resetEvents = resetEvents,
                onProgramGone = {
                    println("$routeProgram GONE!")
                    if (!navController.popBackStack()) {
                        println("Couldn't pop...")
                        navController.navigate(InitialLoad)
                    }
                },
                onDrawerOpen = onDrawerOpen,
                onProgramSettings = {
                    navController.navigate(ProgramSettings(routeProgram))
                },
            )
        }

        composable<LoadGame>(
            deepLinks = listOf(
                navDeepLink {
                    action = Intent.ACTION_VIEW
                    mimeType = "*/*"
                },
            )
        ) { entry ->
            LoadScreen(onProgramLoaded = {
                navController.navigate(PlayGame(it)) {
                    // https://issuetracker.google.com/issues/370694831
                    @SuppressLint("RestrictedApi")
                    popUpTo(entry.toRoute<LoadGame>()) {
                        inclusive = true
                    }
                }
            })
        }
    }
}

val fadeIn = fadeIn(animationSpec = tween(durationMillis = 500))
val fadeOut = fadeOut(animationSpec = tween(durationMillis = 500))
val fullSlideOutToLeft = slideOutHorizontally(targetOffsetX = { -it })
val fullSlideInFromLeft = slideInHorizontally(initialOffsetX = { -it })
val fullSlideOutToRight = slideOutHorizontally(targetOffsetX = { it })
val fullSlideInFromRight = slideInHorizontally(initialOffsetX = { it })
