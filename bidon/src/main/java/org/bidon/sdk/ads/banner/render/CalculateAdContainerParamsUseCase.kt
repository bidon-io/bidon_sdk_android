package org.bidon.sdk.ads.banner.render

import android.graphics.Point
import android.graphics.PointF
import android.view.ViewGroup
import org.bidon.sdk.ads.banner.BannerPosition

internal class CalculateAdContainerParamsUseCase {
    operator fun invoke(
        positionState: AdRenderer.PositionState,
        screenSize: Point,
        bannerWidth: Int,
        bannerHeight: Int,
    ): AdViewsParameters {
        val params = when (positionState) {
            is AdRenderer.PositionState.Coordinate -> positionState.adContainerParams

            is AdRenderer.PositionState.Place -> when (positionState.position) {
                BannerPosition.HorizontalTop -> AdRenderer.AdContainerParams(
                    offset = Point(0, 0),
                    pivot = PointF(0f, 0f),
                    rotation = 0
                )

                BannerPosition.HorizontalBottom -> AdRenderer.AdContainerParams(
                    offset = Point(0, screenSize.y),
                    pivot = PointF(0f, 1f),
                    rotation = 0
                )

                BannerPosition.VerticalLeft -> AdRenderer.AdContainerParams(
                    offset = Point(0, 0),
                    pivot = PointF(0f, 0f),
                    rotation = -90
                )

                BannerPosition.VerticalRight -> AdRenderer.AdContainerParams(
                    offset = Point(screenSize.x, 0),
                    pivot = PointF(1f, 0f),
                    rotation = 90
                )
            }
        }
        val (width, height) = when (positionState) {
            is AdRenderer.PositionState.Coordinate -> bannerWidth to bannerHeight
            is AdRenderer.PositionState.Place -> when (positionState.position) {
                BannerPosition.HorizontalTop,
                BannerPosition.HorizontalBottom -> bannerWidth to bannerHeight

                BannerPosition.VerticalLeft,
                BannerPosition.VerticalRight -> bannerHeight to bannerWidth
            }
        }

        return AdViewsParameters(
            baseParams = params,
            adContainerWidth = width,
            adContainerHeight = height,
            adContainerLayoutParamsWidth = when (positionState) {
                is AdRenderer.PositionState.Coordinate -> width
                is AdRenderer.PositionState.Place -> width.takeIf {
                    positionState.position in arrayOf(
                        BannerPosition.VerticalRight,
                        BannerPosition.VerticalLeft
                    )
                } ?: ViewGroup.LayoutParams.MATCH_PARENT
            },
            adContainerLayoutParamsHeight = when (positionState) {
                is AdRenderer.PositionState.Coordinate -> height
                is AdRenderer.PositionState.Place -> {
                    height.takeIf {
                        positionState.position in arrayOf(
                            BannerPosition.HorizontalTop,
                            BannerPosition.HorizontalBottom
                        )
                    } ?: ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        )
    }
}

internal data class AdViewsParameters(
    val baseParams: AdRenderer.AdContainerParams,
    val adContainerWidth: Int,
    val adContainerHeight: Int,
    val adContainerLayoutParamsWidth: Int,
    val adContainerLayoutParamsHeight: Int,
)