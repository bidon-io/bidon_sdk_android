package org.bidon.sdk.ads.banner

/**
 * Created by Aleksei Cherniaev on 04/09/2023.
 */
public enum class BannerPosition {
    HorizontalTop,
    HorizontalBottom,
    VerticalLeft,
    VerticalRight;

    public companion object {
        public val Default: BannerPosition get() = HorizontalBottom
    }
}
