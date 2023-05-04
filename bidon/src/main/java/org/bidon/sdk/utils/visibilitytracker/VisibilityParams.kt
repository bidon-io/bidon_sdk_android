package org.bidon.sdk.utils.visibilitytracker

internal data class VisibilityParams(
    val timeThresholdMs: Long = DefTimeThresholdMs,
    val pixelThreshold: Float = DefPixelThreshold,
    val maxCountOverlappedViews: Int = DefMaxCountOverlappedViews,
    val isIgnoreWindowFocus: Boolean = DefIgnoreWindowFocus,
    val isIgnoreOverlap: Boolean = DefIgnoreOverlap,
)

private const val DefTimeThresholdMs: Long = 250L
private const val DefPixelThreshold: Float = 0.85f
private const val DefMaxCountOverlappedViews: Int = 3
private const val DefIgnoreWindowFocus: Boolean = false
private const val DefIgnoreOverlap: Boolean = false