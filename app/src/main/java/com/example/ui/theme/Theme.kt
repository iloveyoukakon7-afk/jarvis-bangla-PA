package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ChhotuGoldColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepGold,
    onPrimaryContainer = WarmCream,
    secondary = BrightGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = VelvetElevated,
    onSecondaryContainer = ChampagneGold,
    tertiary = AmberAlert,
    onTertiary = ObsidianBlack,
    error = CrimsonShield,
    onError = WarmCream,
    background = ObsidianBlack,
    onBackground = WarmCream,
    surface = CharcoalPanel,
    onSurface = WarmCream,
    surfaceVariant = VelvetSurface,
    onSurfaceVariant = MutedSand,
    outline = BronzeBorder
)

val ChhotuShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun JarvisBanglaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChhotuGoldColorScheme,
        typography = JarvisTypography,
        shapes = ChhotuShapes,
        content = content
    )
}
