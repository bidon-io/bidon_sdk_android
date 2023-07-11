package org.bidon.sdk.ads.banner.helper

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Aleksei Cherniaev on 27/04/2023.
 */
fun BannerFormat?.getWidthDp() =
    when (this) {
        BannerFormat.Banner -> 320
        BannerFormat.LeaderBoard -> 728
        BannerFormat.MRec -> 300
        BannerFormat.Adaptive,
        null -> WRAP_CONTENT
    }

fun BannerFormat?.getHeightDp() =
    when (this) {
        BannerFormat.Banner -> 50
        BannerFormat.LeaderBoard -> 90
        BannerFormat.MRec -> 250
        BannerFormat.Adaptive,
        null -> WRAP_CONTENT
    }