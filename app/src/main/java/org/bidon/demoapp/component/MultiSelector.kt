package org.bidon.demoapp.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode

@Composable
fun <T> MultiSelector(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    items: List<T>,
    selectedItems: List<T> = emptyList(),
    getItemTitle: (T) -> String,
    onItemClicked: (T) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        title?.let {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        description?.let {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayLarge
            )
        }
        if (items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                mainAxisAlignment = MainAxisAlignment.Center,
                mainAxisSize = SizeMode.Expand,
                crossAxisSpacing = 6.dp,
                mainAxisSpacing = 6.dp,
            ) {
                items.forEach { item ->
                    val isSelected = item in selectedItems
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .clickable {
                                onItemClicked.invoke(item)
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
