package org.bidon.mobilefuse

import android.content.Context
import com.mobilefuse.sdk.MobileFuse
import com.mobilefuse.sdk.MobileFuseSettings
import com.mobilefuse.sdk.SdkInitListener
import org.bidon.mobilefuse.ext.GetMobileFuseTokenUseCase
import org.bidon.mobilefuse.ext.adapterVersion
import org.bidon.mobilefuse.ext.sdkVersion
import org.bidon.mobilefuse.ext.toMobileFusePrivacyPreferences
import org.bidon.mobilefuse.impl.MobileFuseBannerAuctionParams
import org.bidon.mobilefuse.impl.MobileFuseBannerImpl
import org.bidon.mobilefuse.impl.MobileFuseFullscreenAuctionParams
import org.bidon.mobilefuse.impl.MobileFuseInterstitialImpl
import org.bidon.mobilefuse.impl.MobileFuseRewardedAdImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.regulation.Regulation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 06/07/2023.
 */
internal val MobileFuseDemandId = DemandId("mobilefuse")

/**
 * [MobileFuse Documentation](https://docs.mobilefuse.com/docs/android-interstitial-ads)
 */
@Suppress("unused")
internal class MobileFuseAdapter :
    Adapter.Bidding,
    Initializable<MobileFuseParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<MobileFuseBannerAuctionParams>,
    AdProvider.Interstitial<MobileFuseFullscreenAuctionParams>,
    AdProvider.Rewarded<MobileFuseFullscreenAuctionParams> {
    override val demandId: DemandId = MobileFuseDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun getToken(adTypeParam: AdTypeParam): String? =
        GetMobileFuseTokenUseCase(adTypeParam.activity.applicationContext, isTestMode)

    override suspend fun init(context: Context, configParams: MobileFuseParams) = suspendCoroutine { continuation ->
        MobileFuseSettings.setTestMode(isTestMode)
        MobileFuse.init(
            object : SdkInitListener {
                override fun onInitSuccess() {
                    return continuation.resume(Unit)
                }

                override fun onInitError() {
                    return continuation.resumeWithException(
                        BidonError.Unspecified(demandId, Throwable("Error while initialization"))
                    )
                }
            }
        )
    }

    override fun parseConfigParam(json: String): MobileFuseParams = MobileFuseParams()

    override fun updateRegulation(regulation: Regulation) {
        MobileFuse.setPrivacyPreferences(regulation.toMobileFusePrivacyPreferences())
    }

    override fun banner(): AdSource.Banner<MobileFuseBannerAuctionParams> {
        return MobileFuseBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<MobileFuseFullscreenAuctionParams> {
        return MobileFuseInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<MobileFuseFullscreenAuctionParams> {
        return MobileFuseRewardedAdImpl()
    }
}