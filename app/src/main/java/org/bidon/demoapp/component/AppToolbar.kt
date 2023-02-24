package org.bidon.demoapp.component

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun AppToolbar(
    title: String = "",
    icon: ImageVector = Icons.Outlined.ArrowBack,
    onNavigationButtonClicked: () -> Unit
) {
    TopAppBar(
        title = {
            H5Text(text = title)
        },
        backgroundColor = MaterialTheme.colors.background,
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
        elevation = 0.dp
    )
}
