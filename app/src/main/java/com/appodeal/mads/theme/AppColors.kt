package com.appodeal.mads.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

internal val DarkColors = darkColors(
    // Buttons
    primary = AppColors.Orange,
    primaryVariant = AppColors.Dark,
    onPrimary = Color.White,

    secondary = AppColors.GreyD,
    secondaryVariant = AppColors.GreyD,
    onSecondary = Color.White,

    background = AppColors.Dark,
    onBackground = Color.White,

    surface = AppColors.GreyD,
    onSurface = Color.White,

    error = AppColors.Orange,
    onError = Color.White
)
internal val LightColors = lightColors(
    primary = Color.Black,
    primaryVariant = AppColors.Dark,
    onPrimary = Color.White,

    secondary = AppColors.Grey,
    secondaryVariant = AppColors.Grey,
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