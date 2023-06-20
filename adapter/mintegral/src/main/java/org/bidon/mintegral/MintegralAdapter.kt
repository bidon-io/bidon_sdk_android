package org.bidon.mintegral

import android.app.Application
import android.content.Context
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.mintegral.ext.adapterVersion
import org.bidon.mintegral.ext.sdkVersion
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
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
    AdProvider.Banner<MintegralAuctionParam> {

    override val demandId: DemandId = MintegralDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: MintegralInitParam) =
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

    override fun parseConfigParam(json: String): MintegralInitParam {
        val jsonObject = JSONObject(json)
        return MintegralInitParam(
            appId = jsonObject.getString("app_id"),
            appKey = jsonObject.getString("app_key")
        )
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<MintegralAuctionParam> {
        TODO("Not yet implemented")
    }
}

private const val Tag = "MintegralAdapter"