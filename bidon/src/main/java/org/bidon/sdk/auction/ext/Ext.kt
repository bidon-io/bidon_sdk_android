package org.bidon.sdk.auction.ext

import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo

/**
 * Created by Aleksei Cherniaev on 10/11/2023.
 */
val BannerFormat.width
    get() = when (this) {
        BannerFormat.Banner -> 320
        BannerFormat.LeaderBoard -> 728
        BannerFormat.MRec -> 300
        BannerFormat.Adaptive -> if (DeviceInfo.isTablet) 728 else 320
    }
val BannerFormat.height
    get() = when (this) {
        BannerFormat.Banner -> 50
        BannerFormat.LeaderBoard -> 90
        BannerFormat.MRec -> 250
        BannerFormat.Adaptive -> if (DeviceInfo.isTablet) 90 else 50
    }