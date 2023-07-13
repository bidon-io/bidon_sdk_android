package org.bidon.demoapp.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.SizeMode

@Composable
fun <T> HorizontalItemSelector(
    modifier: Modifier = Modifier,
    title: String? = null,
    items: List<T>,
    selectedItem: T? = null,
    getItemTitle: (T) -> String,
    onItemClicked: (T) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        title?.let {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                mainAxisSize = SizeMode.Expand,
                crossAxisSpacing = 6.dp,
                mainAxisSpacing = 6.dp,
            ) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .clickable {
                                if (!isSelected) {
                                    onItemClicked.invoke(item)
                                }
                            },
                        border = BorderStroke(
                            width = 1.dp,
                            color = color
                        ),
                        shadowElevation = if (isSelected) 16.dp else 0.dp
                    ) {
                        Text(
                            text = getItemTitle(item),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 9.sp,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
