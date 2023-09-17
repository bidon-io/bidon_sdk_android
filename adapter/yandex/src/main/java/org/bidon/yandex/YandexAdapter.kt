package org.bidon.yandex

import android.content.Context
import com.yandex.mobile.ads.common.MobileAds
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.regulation.Regulation
import org.bidon.yandex.ext.adapterVersion
import org.bidon.yandex.ext.sdkVersion
import org.bidon.yandex.impl.YandexBannerAuctionParam
import org.bidon.yandex.impl.YandexBannerImpl
import org.bidon.yandex.impl.YandexFullscreenAuctionParam
import org.bidon.yandex.impl.YandexInterstitialImpl
import kotlin.coroutines.resume

/**
 * Created by Aleksei Cherniaev on 17/09/2023.
 */
internal val YandexDemandId = DemandId("yandex")

class YandexAdapter :
    Adapter,
    Initializable<YandexParameters>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<YandexBannerAuctionParam>,
    AdProvider.Interstitial<YandexFullscreenAuctionParam>,
    AdProvider.Rewarded<YandexFullscreenAuctionParam> {
    override val demandId: DemandId get() = YandexDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: YandexParameters) = suspendCancellableCoroutine {
        MobileAds.enableLogging(isTestMode)
        MobileAds.initialize(context) {
            it.resume(Unit)
        }
    }

    override fun parseConfigParam(json: String): YandexParameters = YandexParameters

    override fun updateRegulation(regulation: Regulation) {
        MobileAds.setUserConsent(regulation.gdprConsent)
        MobileAds.setAgeRestrictedUser(regulation.coppaApplies)
    }

    override fun banner(): AdSource.Banner<YandexBannerAuctionParam> {
        return YandexBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<YandexFullscreenAuctionParam> {
        return YandexInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<YandexFullscreenAuctionParam> {
        TODO("Not yet implemented")
    }
}