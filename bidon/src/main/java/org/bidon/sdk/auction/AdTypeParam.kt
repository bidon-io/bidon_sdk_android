package org.bidon.sdk.auction

import android.app.Activity
import android.view.ViewGroup
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal sealed interface AdTypeParam {
    val pricefloor: Double

    class Banner(
        val adContainer: ViewGroup,
        val bannerFormat: BannerFormat,
        override val pricefloor: Double,
    ) : AdTypeParam

    class Interstitial(
        val activity: Activity,
        override val pricefloor: Double,
    ) : AdTypeParam

    class Rewarded(
        val activity: Activity,
        override val pricefloor: Double,
    ) : AdTypeParam
}
