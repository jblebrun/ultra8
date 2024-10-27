package com.emerjbl.ultra8.util

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute


inline fun <reified T : Any> NavBackStackEntry?.matchesRoute(route: T): Boolean =
    if (this?.destination?.hasRoute<T>() == true) {
        toRoute<T>() == route
    } else {
        false
    }
