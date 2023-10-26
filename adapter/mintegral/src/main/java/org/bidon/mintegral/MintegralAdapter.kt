package org.bidon.mintegral

import android.app.Application
import android.content.Context
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bidon.mintegral.ext.adapterVersion
import org.bidon.mintegral.ext.sdkVersion
import org.bidon.mintegral.impl.MintegralBannerImpl
import org.bidon.mintegral.impl.MintegralInterstitialImpl
import org.bidon.mintegral.impl.MintegralRewardedImpl
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
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
    SupportsRegulation,
    Initializable<MintegralInitParam>,
    AdProvider.Banner<MintegralBannerAuctionParam>,
    AdProvider.Interstitial<MintegralAuctionParam>,
    AdProvider.Rewarded<MintegralAuctionParam> {

    private var context: Context? = null
    override val demandId: DemandId = MintegralDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: MintegralInitParam) =
        withContext(SdkDispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                this@MintegralAdapter.context = context
                val sdk = MBridgeSDKFactory.getMBridgeSDK()
                val configurationMap = sdk.getMBConfigurationMap(configParams.appId, configParams.appKey)
                updateRegulation(BidonSdk.regulation)
                sdk.init(
                    configurationMap, context.applicationContext as Application,
                    object : SDKInitStatusListener {
                        override fun onInitSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onInitFail(message: String?) {
                            logError(TAG, "Error while initialization: $message", BidonError.Unspecified(demandId))
                            continuation.resumeWithException(BidonError.Unspecified(demandId))
                        }
                    }
                )
            }
        }

    override fun parseConfigParam(json: String): MintegralInitParam {
        val jsonObject = JSONObject(json)
        return MintegralInitParam(
            appId = jsonObject.getString("app_id"),
            appKey = jsonObject.getString("app_key")
        )
    }

    override fun banner(): AdSource.Banner<MintegralBannerAuctionParam> {
        return MintegralBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<MintegralAuctionParam> {
        return MintegralInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<MintegralAuctionParam> {
        return MintegralRewardedImpl()
    }

    override fun updateRegulation(regulation: Regulation) {
        context?.let { context ->
            val sdk = MBridgeSDKFactory.getMBridgeSDK()
            if (regulation.gdprApplies) {
                val consent = if (regulation.hasGdprConsent) {
                    MBridgeConstans.IS_SWITCH_ON
                } else {
                    MBridgeConstans.IS_SWITCH_OFF
                }
                sdk.setUserPrivateInfoType(context, MBridgeConstans.AUTHORITY_ALL_INFO, consent)
            }
            if (regulation.ccpaApplies) {
                sdk.setDoNotTrackStatus(context, regulation.hasCcpaConsent.not())
            }
            if (regulation.coppaApplies) {
                sdk.setCoppaStatus(context, true)
            }
        }
    }
}

private const val TAG = "MintegralAdapter"