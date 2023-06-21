package org.bidon.mintegral

import android.app.Application
import android.content.Context
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bidon.mintegral.ext.adapterVersion
import org.bidon.mintegral.ext.sdkVersion
import org.bidon.mintegral.impl.MintegralInterstitialImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.SdkDispatchers
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal val MintegralDemandId = DemandId("mintegral")

class MintegralAdapter :
    Adapter,
    Initializable<MintegralInitParam>,
    AdProvider.Banner<MintegralAuctionParam>,
    AdProvider.Interstitial<MintegralAuctionParam>,
    AdProvider.Rewarded<MintegralAuctionParam> {

    override val demandId: DemandId = MintegralDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: MintegralInitParam) =
        withContext(SdkDispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val sdk = MBridgeSDKFactory.getMBridgeSDK()
                val configurationMap = sdk.getMBConfigurationMap(configParams.appId, configParams.appKey)
                sdk.init(
                    configurationMap, context.applicationContext as Application,
                    object : SDKInitStatusListener {
                        override fun onInitSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onInitFail(message: String?) {
                            logError(Tag, "Error while initialization: $message", BidonError.Unspecified(demandId))
                            continuation.resumeWithException(BidonError.Unspecified(demandId))
                        }
                    }
                )
            }
        }

    override fun parseConfigParam(json: String): MintegralInitParam {
        val jsonObject = JSONObject(json)
        return MintegralInitParam(
            appId = jsonObject.optString("app_id") ?: "144002",
            appKey = jsonObject.optString("app_key") ?: "7c22942b749fe6a6e361b675e96b3ee9"
        )
    }

    override fun banner(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Banner<MintegralAuctionParam> {
        TODO("Not yet implemented")
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<MintegralAuctionParam> {
        return MintegralInterstitialImpl(demandId, demandAd, roundId, auctionId)
    }

    override fun rewarded(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Rewarded<MintegralAuctionParam> {
        TODO("Not yet implemented")
    }
}

private const val Tag = "MintegralAdapter"