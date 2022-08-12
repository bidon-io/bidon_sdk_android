package com.appodeal.mads.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal enum class ItemPosition {
    First,
    Middle,
    Last,
    Single
}

internal fun <T> List<T>.getShapeByPositionFor(element: T) = when {
    this.size == 1 -> ItemPosition.Single
    element == this.first() -> ItemPosition.First
    element == this.last() -> ItemPosition.Last
    else -> ItemPosition.Middle
}.shape()

internal fun ItemPosition.shape() = when (this) {
    ItemPosition.First -> RoundedCornerShape(
        topStart = ItemCornerRadius,
        topEnd = ItemCornerRadius,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )
    ItemPosition.Middle -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    ItemPosition.Last -> RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomEnd = ItemCornerRadius,
        bottomStart = ItemCornerRadius
    )
    ItemPosition.Single -> RoundedCornerShape(
        topStart = ItemCornerRadius,
        topEnd = ItemCornerRadius,
        bottomEnd = ItemCornerRadius,
        bottomStart = ItemCornerRadius
    )
}

private val ItemCornerRadius = 4.dp