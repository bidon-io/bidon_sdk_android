package org.bidon.sdk.auction

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed interface AdTypeParam {
    public val activity: Activity
    public val pricefloor: Double
    public val auctionKey: String?

    public class Banner(
        override val activity: Activity,
        override val pricefloor: Double,
        override val auctionKey: String?,
        public val bannerFormat: BannerFormat,
        public val containerWidth: Float,
    ) : AdTypeParam

    public class Interstitial(
        override val activity: Activity,
        override val pricefloor: Double,
        override val auctionKey: String?,
    ) : AdTypeParam

    public class Rewarded(
        override val activity: Activity,
        override val pricefloor: Double,
        override val auctionKey: String?,
    ) : AdTypeParam
}
