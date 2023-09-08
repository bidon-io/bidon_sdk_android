package org.bidon.sdk.ads.banner

/**
 * Created by Aleksei Cherniaev on 04/09/2023.
 */
enum class BannerPosition {
    HorizontalTop,
    HorizontalBottom,
    VerticalLeft,
    VerticalRight;

    companion object {
        val Default get() = HorizontalBottom
    }
}
