package org.bidon.sdk.ads.banner.render

import android.content.Context

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
@Deprecated("")
internal class BannerDisplayContainer @JvmOverloads constructor(
    context: Context,
    private val isRotated: Boolean = false,
    useSafeArea: Boolean = true
) : SafeInsetsFrameLayout(context, useSafeArea) {

    override fun adjustInsetsCenter(): Boolean {
        return !isRotated
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        var resolvedWidth = 0
        var resolvedHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val targetWidth = MeasureSpec.getSize(widthMeasureSpec)
            val targetHeight = MeasureSpec.getSize(heightMeasureSpec)
            val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY)
            if (isRotated) {
                val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.AT_MOST)
                measureChild(child, childHeightMeasureSpec, childWidthMeasureSpec)
                resolvedWidth = maxOf(resolvedWidth, child.measuredHeight)
                resolvedHeight = maxOf(resolvedHeight, child.measuredWidth)
            } else {
                val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.UNSPECIFIED)
                measureChild(child, childWidthMeasureSpec, childHeightMeasureSpec)
                resolvedWidth = maxOf(resolvedWidth, child.measuredWidth)
                resolvedHeight = maxOf(resolvedHeight, child.measuredHeight)
            }
        }
        setMeasuredDimension(
            resolvedWidth + paddingLeft + paddingRight,
            resolvedHeight + paddingTop + paddingBottom
        )
    }
}