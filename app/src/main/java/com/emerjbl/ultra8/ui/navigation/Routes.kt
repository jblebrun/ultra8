package com.emerjbl.ultra8.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route

@Serializable
data class PlayGame(val programName: String) : Route

@Serializable
data object LoadGame : Route

@Serializable
data object InitialLoad : Route

@Serializable
data class ProgramSettings(val programName: String) : Route

@Serializable
data object Catalog
