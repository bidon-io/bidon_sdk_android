package com.appodeal.mads.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

//@Composable
//internal fun SetSystemBarColors() {
//    val systemUiController = rememberSystemUiController()
//    val useDarkIcons = MaterialTheme.colors.isLight
//    val bgColor = MaterialTheme.colors.background
//    SideEffect {
//        systemUiController.setSystemBarsColor(
//            color = bgColor,
//            darkIcons = useDarkIcons
//        )
//    }
//}