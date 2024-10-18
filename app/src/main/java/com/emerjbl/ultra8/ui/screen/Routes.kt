package com.emerjbl.ultra8.ui.screen

import kotlinx.serialization.Serializable

@Serializable
data class PlayGame(val programName: String)

@Serializable
data object LoadGame
