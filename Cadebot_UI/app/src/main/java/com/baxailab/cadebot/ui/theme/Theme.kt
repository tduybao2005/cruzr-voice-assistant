package com.baxailab.cadebot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Mộc LamColorScheme = lightColorScheme(
    primary = Mộc LamEspresso,
    onPrimary = Mộc LamOnDark,
    primaryContainer = Mộc LamCoffee,
    onPrimaryContainer = Mộc LamCream,
    secondary = Mộc LamCaramel,
    onSecondary = Mộc LamEspresso,
    secondaryContainer = Mộc LamLatte,
    onSecondaryContainer = Mộc LamEspresso,
    tertiary = Mộc LamCoffee,
    background = Mộc LamFoam,
    onBackground = Mộc LamEspresso,
    surface = Mộc LamSurface,
    onSurface = Mộc LamEspresso,
    surfaceVariant = Mộc LamCream,
    onSurfaceVariant = Mộc LamCoffee,
    error = Mộc LamError,
    outline = Mộc LamLatte
)

@Composable
fun CadebotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Mộc LamColorScheme,
        typography = Mộc LamTypography,
        content = content
    )
}
