package com.baxailab.cadebot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MocLamColorScheme = lightColorScheme(
    primary = MocLamEspresso,
    onPrimary = MocLamOnDark,
    primaryContainer = MocLamCoffee,
    onPrimaryContainer = MocLamCream,
    secondary = MocLamCaramel,
    onSecondary = MocLamEspresso,
    secondaryContainer = MocLamLatte,
    onSecondaryContainer = MocLamEspresso,
    tertiary = MocLamCoffee,
    background = MocLamFoam,
    onBackground = MocLamEspresso,
    surface = MocLamSurface,
    onSurface = MocLamEspresso,
    surfaceVariant = MocLamCream,
    onSurfaceVariant = MocLamCoffee,
    error = MocLamError,
    outline = MocLamLatte
)

@Composable
fun CadebotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MocLamColorScheme,
        typography = MocLamTypography,
        content = content
    )
}
