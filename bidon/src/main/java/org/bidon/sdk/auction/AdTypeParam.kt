package org.bidon.sdk.auction

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed interface AdTypeParam {
    val activity: Activity
    val pricefloor: Double

    class Banner(
        override val activity: Activity,
        val bannerFormat: BannerFormat,
        override val pricefloor: Double,
        val containerWidth: Float,
    ) : AdTypeParam

    class Interstitial(
        override val activity: Activity,
        override val pricefloor: Double,
    ) : AdTypeParam

    class Rewarded(
        override val activity: Activity,
        override val pricefloor: Double,
    ) : AdTypeParam
}
