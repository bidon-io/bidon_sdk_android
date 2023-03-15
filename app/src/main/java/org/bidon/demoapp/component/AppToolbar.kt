package org.bidon.demoapp.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppToolbar(
    title: String = "",
    icon: ImageVector = Icons.Outlined.ArrowBack,
    onNavigationButtonClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Subtitle1Text(text = title)
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(MaterialTheme.colorScheme.background),
        navigationIcon = {
            IconButton(
                onClick = {
                    onNavigationButtonClicked.invoke()
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                )
            }
        },
    )
}
