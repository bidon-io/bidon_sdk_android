package org.bidon.demoapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val DarkColors = darkColorScheme(
    // Buttons
    primary = AppColors.Orange,
    onPrimary = Color.White,
    primaryContainer = AppColors.Orange,

    secondary = AppColors.GreyD,
    onSecondary = Color.White,
    secondaryContainer = AppColors.Orange,

    background = AppColors.Dark,
    onBackground = Color.White,

    surface = AppColors.GreyD,
    onSurface = Color.White,

    error = AppColors.Orange,
    onError = Color.White,

    tertiaryContainer = AppColors.Orange,
    tertiary = AppColors.Orange,
    onTertiary = AppColors.Orange,
    onTertiaryContainer = AppColors.Orange,
    surfaceVariant = AppColors.Orange,
    outline = AppColors.Orange,
    outlineVariant = AppColors.Orange,
)
internal val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,

    secondary = AppColors.Grey,
    onSecondary = Color.White,

    background = AppColors.GreyL,
    onBackground = Color.Black,

    surface = AppColors.Grey,
    onSurface = Color.White,

    error = AppColors.Red,
    onError = Color.White
)

object AppColors {
    val Orange = Color(0xFFF3943D)
    val Red = Color(0xFFE84039)
    val Dark = Color(0xFF223551)
    val GreyD = Color(0xFF6E7B8D)
    val Grey = Color(0xFFBCC2CC)
    val GreyL = Color(0xFFE6EBF2)
}
