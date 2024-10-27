package com.emerjbl.ultra8.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data class PlayGame(val programName: String)

@Serializable
data object LoadGame

@Serializable
data object InitialLoad

@Serializable
data object Catalog
