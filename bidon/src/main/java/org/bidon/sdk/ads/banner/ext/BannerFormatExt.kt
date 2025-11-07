package org.bidon.sdk.ads.banner.ext

import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo

public val BannerFormat.width: Int
    get() = when (this) {
        BannerFormat.Banner -> 320
        BannerFormat.LeaderBoard -> 728
        BannerFormat.MRec -> 300
        BannerFormat.Adaptive -> DeviceInfo.screenWidthDp
    }

public val BannerFormat.height: Int
    get() = when (this) {
        BannerFormat.Banner -> 50
        BannerFormat.LeaderBoard -> 90
        BannerFormat.MRec -> 250
        BannerFormat.Adaptive -> if (DeviceInfo.isTablet) 90 else 50
    }