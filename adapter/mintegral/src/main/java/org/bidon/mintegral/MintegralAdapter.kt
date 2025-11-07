package org.bidon.mintegral

import android.app.Application
import android.content.Context
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bidon.mintegral.ext.adapterVersion
import org.bidon.mintegral.ext.sdkVersion
import org.bidon.mintegral.impl.MintegralBannerImpl
import org.bidon.mintegral.impl.MintegralInterstitialImpl
import org.bidon.mintegral.impl.MintegralRewardedImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral](https://dev.mintegral.com/doc/)
 */
internal val MintegralDemandId = DemandId("mintegral")

@Suppress("unused")
internal class MintegralAdapter :
    Adapter.Bidding,
    Adapter.Network,
    SupportsRegulation,
    Initializable<MintegralInitParam>,
    AdProvider.Banner<MintegralBannerAuctionParam>,
    AdProvider.Interstitial<MintegralAuctionParam>,
    AdProvider.Rewarded<MintegralAuctionParam> {

    override val demandId: DemandId = MintegralDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    private val isInitialized: AtomicBoolean = AtomicBoolean(false)
    private var context: Context? = null

    override suspend fun getToken(adTypeParam: AdTypeParam): String? =
        BidManager.getBuyerUid(adTypeParam.activity.applicationContext)

    override suspend fun init(context: Context, configParams: MintegralInitParam) {
        if (isInitialized.get()) return

        withContext(Dispatchers.Main.immediate) {
            suspendCoroutine { continuation ->
                this@MintegralAdapter.context = context
                val sdk = MBridgeSDKFactory.getMBridgeSDK()
                val configurationMap = sdk.getMBConfigurationMap(configParams.appId, configParams.appKey)
                sdk.init(
                    configurationMap,
                    context.applicationContext as Application,
                    object : SDKInitStatusListener {
                        override fun onInitSuccess() {
                            if (isInitialized.compareAndSet(false, true)) {
                                continuation.resume(Unit)
                            }
                        }

                        override fun onInitFail(message: String?) {
                            if (isInitialized.compareAndSet(false, true)) {
                                val error = BidonError.Unspecified(demandId, Throwable(message))
                                logError(TAG, "Error while initialization: $message", error)
                                continuation.resumeWithException(error)
                            }
                        }
                    }
                )
            }
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