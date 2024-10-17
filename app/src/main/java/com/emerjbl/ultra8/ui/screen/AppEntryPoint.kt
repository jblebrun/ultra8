package com.emerjbl.ultra8.ui.screen

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import kotlinx.serialization.Serializable

@Serializable
data class PlayGame(val programName: String)

@Serializable
data object LoadGame

@Composable
fun AppEntryPoint() {
    val navController = rememberNavController()

    Ultra8Theme {
        NavHost(navController, startDestination = PlayGame("breakout")) {
            composable<PlayGame> {
                PlayScreen(
                    onSelectProgram = { navController.navigate(PlayGame(it)) }
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
                LoadScreen(onProgramLoaded = {
                    navController.navigate(PlayGame(it)) {
                        popUpTo(LoadGame) { inclusive = true }
                    }
                })
            }
        }
    }
}
