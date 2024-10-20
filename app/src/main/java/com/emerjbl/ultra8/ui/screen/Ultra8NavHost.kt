package com.emerjbl.ultra8.ui.screen

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow

@Composable
fun Ultra8NavHost(
    navController: NavHostController,
    gameShouldRun: Boolean,
    resetEvents: Flow<Unit>,
    onDrawerOpen: () -> Unit,
) {
    NavHost(navController, startDestination = PlayGame("breakout")) {
        composable<PlayGame> { entry ->
            PlayScreen(
                programName = entry.toRoute<PlayGame>().programName,
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
        ) {
            LoadScreen(onProgramLoaded = { navController.navigate(PlayGame(it)) })
        }
    }
}
