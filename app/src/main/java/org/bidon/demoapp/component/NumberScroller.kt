package org.bidon.demoapp.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberScroller(
    modifier: Modifier = Modifier,
    initialValue: Float,
    value: String,
    onMinusClicked: () -> Unit,
    onPlusClicked: () -> Unit,
    onValueChanges: (Float) -> Unit,
) {
    val offset = remember { mutableStateOf(initialValue) }
    Row(
        modifier = modifier.scrollable(
            orientation = Orientation.Horizontal,
            state = rememberScrollableState { delta ->
                offset.value += (delta / 10).toInt()
                onValueChanges.invoke(offset.value)
                delta
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onMinusClicked() },
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                modifier = Modifier.size(40.dp),
                contentDescription = "Minus",
                tint = MaterialTheme.colorScheme.error
            )
        }
        Subtitle1Text(text = value)
        IconButton(
            onClick = { onPlusClicked() }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                modifier = Modifier.size(40.dp),
                contentDescription = "Plus",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}