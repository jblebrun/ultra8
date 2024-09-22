package com.emerjbl.ultra8.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

class Chip8ColorScheme(
    val keypadBackground: Color,
    val keyCapBackground: Color,
    val keyCapForeground: Color,
    val pixelColor: Color,
)

private val DefaultChip8ColorScheme = Chip8ColorScheme(
    keypadBackground = Gray220,
    keyCapBackground = Gray200,
    keyCapForeground = Gray40,
    pixelColor = LightColorScheme.tertiary

)

val LocalChip8ColorScheme = compositionLocalOf { DefaultChip8ColorScheme }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.chip8Colors: Chip8ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalChip8ColorScheme.current

@Composable
fun Ultra8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val chip8ColorScheme = Chip8ColorScheme(
        keypadBackground = colorScheme.secondaryContainer,
        keyCapBackground = colorScheme.secondary,
        keyCapForeground = colorScheme.onSecondary,
        pixelColor = colorScheme.tertiary
    )

    CompositionLocalProvider(
        LocalChip8ColorScheme provides chip8ColorScheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
