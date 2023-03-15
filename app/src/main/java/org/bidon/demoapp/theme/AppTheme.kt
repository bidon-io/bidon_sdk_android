package org.bidon.demoapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun AppTheme(
    darkTheme: Boolean = true, // isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// @Composable
// internal fun SetSystemBarColors() {
//    val systemUiController = rememberSystemUiController()
//    val useDarkIcons = MaterialTheme.colors.isLight
//    val bgColor = MaterialTheme.colors.background
//    SideEffect {
//        systemUiController.setSystemBarsColor(
//            color = bgColor,
//            darkIcons = useDarkIcons
//        )
//    }
// }
