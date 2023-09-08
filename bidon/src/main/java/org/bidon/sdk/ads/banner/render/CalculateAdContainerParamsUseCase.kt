package org.bidon.sdk.ads.banner.render

import android.graphics.Point
import android.graphics.PointF
import org.bidon.sdk.ads.banner.BannerPosition

internal class CalculateAdContainerParamsUseCase {
    operator fun invoke(
        position: BannerPosition,
        screenSize: Point,
        bannerHeight: Int,
    ): AdRenderer.AdContainerParams {
        val (pivotX, pivotY, rotation) = when (position) {
            BannerPosition.HorizontalTop -> Triple(0.5f, 0f, 0)
            BannerPosition.HorizontalBottom -> Triple(0.5f, 1f, 0)
            BannerPosition.VerticalLeft -> Triple(0.5f, 0.5f, -90)
            BannerPosition.VerticalRight -> Triple(0.5f, 0.5f, 90)
        }
        return AdRenderer.AdContainerParams(
            rotation = rotation,
            offset = when (position) {
                BannerPosition.HorizontalTop -> Point(screenSize.x / 2, 0)
                BannerPosition.HorizontalBottom -> Point(screenSize.x / 2, screenSize.y)
                BannerPosition.VerticalLeft -> Point(bannerHeight / 2, screenSize.y / 2)
                BannerPosition.VerticalRight -> Point(screenSize.x - bannerHeight / 2, screenSize.y / 2)
            },
            pivot = PointF(pivotX, pivotY)
        )
    }
}
