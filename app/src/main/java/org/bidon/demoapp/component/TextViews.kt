package org.bidon.demoapp.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import org.bidon.demoapp.theme.AppTypography

@Composable
fun H5Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.headlineLarge,
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
        style = AppTypography.headlineMedium,
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
        style = AppTypography.titleLarge,
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
        style = AppTypography.titleSmall,
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
        style = AppTypography.bodyLarge,
        color = Color.White
    )
}

@Composable
fun Body2Text(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 10,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.bodySmall,
        color = Color.White,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 10,
    color: Color = Color.White
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.labelLarge,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
