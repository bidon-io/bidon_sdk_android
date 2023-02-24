package org.bidon.demoapp.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun Alert(
    title: String? = null,
    text: String? = null,
    confirmButton: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButton: String? = null,
    onDismiss: (() -> Unit)? = null,
    isCancellable: Boolean = true
) {
    val titleView = @Composable {
        Text(
            text = requireNotNull(title),
            modifier = Modifier.padding(vertical = if (text.isNullOrEmpty()) 10.dp else 0.dp)
        )
    }
    val messageView = @Composable {
        Text(text = requireNotNull(text))
    }
    val nnull: @Composable (() -> Unit)? = null
    AlertDialog(
        onDismissRequest = {
            onDismiss?.invoke()
        },
        title = if (title.isNullOrBlank()) nnull else titleView,
        text = if (text.isNullOrBlank()) nnull else messageView,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm?.invoke()
                }
            ) {
                Text(text = confirmButton!!)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss?.invoke()
                }
            ) {
                Text(text = dismissButton!!)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = isCancellable,
            dismissOnClickOutside = isCancellable
        ),
        modifier = Modifier.padding(24.dp),
        shape = AppShapes.large
    )
}
