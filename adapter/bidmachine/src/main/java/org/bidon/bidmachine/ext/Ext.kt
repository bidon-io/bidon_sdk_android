package org.bidon.bidmachine.ext

import io.bidmachine.AdsFormat
import io.bidmachine.BidMachine
import io.bidmachine.banner.BannerSize
import org.bidon.bidmachine.BidMachineBannerSize
import org.bidon.bidmachine.BuildConfig
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.AdTypeParam

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = BidMachine.VERSION

internal fun AdTypeParam.toBidmachineAdFormat(): AdsFormat = when (this) {
    is AdTypeParam.Banner -> this.bannerFormat.toBidmachineBannerAdFormat()
    is AdTypeParam.Interstitial -> AdsFormat.Interstitial
    is AdTypeParam.Rewarded -> AdsFormat.Rewarded
}

private fun BannerFormat.toBidmachineBannerAdFormat(): AdsFormat = when (this) {
    BannerFormat.Banner -> AdsFormat.Banner_320x50
    BannerFormat.LeaderBoard -> AdsFormat.Banner_728x90
    BannerFormat.MRec -> AdsFormat.Banner_300x250
    BannerFormat.Adaptive -> if (DeviceInfo.isTablet) {
        AdsFormat.Banner_728x90
    } else {
        AdsFormat.Banner_320x50
    }
}

internal fun BannerFormat.asBidMachineBannerSize(): BannerSize = when (this) {
    BannerFormat.Banner -> BidMachineBannerSize.Size_320x50
    BannerFormat.LeaderBoard -> BidMachineBannerSize.Size_728x90
    BannerFormat.MRec -> BidMachineBannerSize.Size_300x250
    BannerFormat.Adaptive -> if (DeviceInfo.isTablet) {
        BidMachineBannerSize.Size_728x90
    } else {
        BidMachineBannerSize.Size_320x50
    }
}