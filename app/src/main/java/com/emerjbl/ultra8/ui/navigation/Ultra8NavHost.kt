package com.emerjbl.ultra8.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.emerjbl.ultra8.ui.gameplay.PlayScreen
import com.emerjbl.ultra8.ui.loading.InitialLoadScreen
import com.emerjbl.ultra8.ui.loading.LoadScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun Ultra8NavHost(
    navController: NavHostController,
    selectedProgram: String,
    gameShouldRun: Boolean,
    resetEvents: Flow<Unit>,
    onDrawerOpen: () -> Unit,
    onProgramLoad: (String) -> Unit,
) {
    NavHost(navController, startDestination = InitialLoad) {
        composable<InitialLoad> {
            InitialLoadScreen(selectedProgram) {
                navController.navigate(PlayGame(selectedProgram)) {
                    // https://issuetracker.google.com/issues/370694831
                    @SuppressLint("RestrictedApi")
                    popUpTo(InitialLoad) {
                        inclusive = true
                    }
                }
            }

        }
        composable<PlayGame> { entry ->
            val routeProgram = entry.toRoute<PlayGame>().programName
            // Prevent game running when view is animating away.
            val gameShouldRun =
                gameShouldRun && entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            PlayScreen(
                programName = routeProgram,
                gameShouldRun = gameShouldRun,
                resetEvents = resetEvents,
                onDrawerOpen = onDrawerOpen,
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
                onProgramLoad(it)
            })
        }
    }
}
