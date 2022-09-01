package com.appodeal.mads.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode

@Composable
fun <T> ItemSelector(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    items: List<T>,
    selectedItem: T? = null,
    getItemTitle: (T) -> String,
    onItemClicked: (T) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        title?.let {
            Text(
                text = title,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.body2
            )
        }
        description?.let {
            Text(
                text = description,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.caption
            )
        }
        if (items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                mainAxisAlignment = MainAxisAlignment.Start,
                mainAxisSize = SizeMode.Expand,
                crossAxisSpacing = 6.dp,
                mainAxisSpacing = 6.dp,
            ) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    val color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colors.background,
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
                        elevation = if (isSelected) 16.dp else 0.dp
                    ) {
                        Text(
                            text = getItemTitle(item),
                            style = MaterialTheme.typography.button,
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
