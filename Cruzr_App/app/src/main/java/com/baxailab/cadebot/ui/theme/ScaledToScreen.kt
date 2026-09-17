package com.baxailab.cadebot.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Fits the existing design onto a shorter panel by scaling it, not by editing it.
 *
 * The screens were laid out for a taller device: Home asks for roughly 670dp of
 * height and the order-success screen about 650dp, while Cruzr's 1920x1080
 * panel at 320dpi only offers 540dp. The first attempt at this trimmed
 * individual paddings and icon sizes screen by screen, which fixed the clipping
 * but quietly changed the proportions the design was drawn with, differently on
 * each screen.
 *
 * Overriding the density instead leaves every `dp` and `sp` value in the UI code
 * untouched and shrinks all of them by the same factor, so the layout keeps its
 * exact proportions and simply renders smaller. One knob, applied identically
 * everywhere — including screens nobody has re-measured.
 *
 * The factor is derived, not hand-picked: dividing the panel's height by
 * [DESIGN_HEIGHT_DP] yields the scale at which the tallest screen just fits.
 * It is capped at 1f, so a device with enough room renders at its native
 * density and nothing changes.
 */
@Composable
fun ScaledToScreen(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val scale = (configuration.screenHeightDp / DESIGN_HEIGHT_DP).coerceAtMost(1f)

    CompositionLocalProvider(
        // Scaling `density` scales sp as well as dp, which is what keeps text in
        // proportion with the boxes around it.
        LocalDensity provides Density(density.density * scale, density.fontScale)
    ) {
        content()
    }
}

/**
 * Height in dp the screens were drawn against — the tallest of them (Home, at
 * ~670dp) plus a little slack. On Cruzr's 540dp panel this gives a scale of
 * about 0.79.
 */
private const val DESIGN_HEIGHT_DP = 690f
