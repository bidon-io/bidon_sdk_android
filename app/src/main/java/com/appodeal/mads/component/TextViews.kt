package com.appodeal.mads.component

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.appodeal.mads.theme.AppTypography

@Composable
fun H5Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.h5,
        color = Color.White
    )
}

@Composable
fun H4Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.h4,
        color = Color.White
    )
}

@Composable
fun Subtitle1Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.subtitle1,
        color = Color.White
    )
}

@Composable
fun Subtitle2Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.subtitle2,
        color = Color.White
    )
}

@Composable
fun Body1Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.body1,
        color = Color.White
    )
}

@Composable
fun Body2Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.body2,
        color = Color.White
    )
}