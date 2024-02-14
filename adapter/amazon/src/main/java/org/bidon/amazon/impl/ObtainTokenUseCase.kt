package org.bidon.amazon.impl

import com.amazon.device.ads.AdError
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.amazon.SlotType
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation
import kotlin.coroutines.resume

internal class AmazonInfo(
    val dtbAdResponse: DTBAdResponse,
    val adSizes: DTBAdSize,
)

internal class ObtainTokenUseCase {

    private val regulation: Regulation
        get() = BidonSdk.regulation

    suspend operator fun invoke(slots: Map<SlotType, List<String>>, adTypeParam: AdTypeParam): List<AmazonInfo> {
        val filteredSlots = slots.filterBy(adTypeParam)
        return obtainInfo(
            adSizes = getAmazonSizes(filteredSlots, adTypeParam)
        )
    }

    private suspend fun obtainInfo(adSizes: List<Pair<SlotType, DTBAdSize>>): List<AmazonInfo> = coroutineScope {
        val results = mutableListOf<AmazonInfo>()
        adSizes
            .map { (slotType, dtbAdSize) ->
                logInfo(TAG, "AmazonInfo request->  $slotType: ${dtbAdSize.slotUUID}")
                dtbAdSize to async { getDTBAdResponse(dtbAdSize) }
            }.forEach { (dtbAdSize, deferred) ->
                val dtbAdResponse = deferred.await()
                if (dtbAdResponse != null) {
                    results.add(AmazonInfo(dtbAdResponse, dtbAdSize))
                    logInfo(TAG, "AmazonInfo added -> ${dtbAdSize.slotUUID}: ${dtbAdResponse.getPricePoints(dtbAdSize)}")
                }
            }
        results
    }

    private suspend fun getDTBAdResponse(adSize: DTBAdSize): DTBAdResponse? = suspendCancellableCoroutine { continuation ->
        val loader = DTBAdRequest()
        loader.applyRegulation(regulation)
        loader.setSizes(adSize)
        loader.loadAd(object : DTBAdCallback {
            override fun onFailure(adError: AdError) {
                logError(TAG, "Error while loading ad: ${adError.code} ${adError.message}", BidonError.NoBid)
                /**Please implement the logic to send ad request without our parameters if you want to
                 * show ads from other ad networks when Amazon ad request fails */
                continuation.resume(null)
            }

            override fun onSuccess(dtbAdResponse: DTBAdResponse) {
                continuation.resume(dtbAdResponse)
            }
        })
    }

    private fun Map<SlotType, List<String>>.filterBy(adTypeParam: AdTypeParam): Map<SlotType, List<String>> {
        return this.mapNotNull { (type, slotUuids) ->
            when (adTypeParam) {
                is AdTypeParam.Banner -> {
                    slotUuids.takeIf {
                        when (adTypeParam.bannerFormat) {
                            BannerFormat.Banner -> type == SlotType.BANNER
                            BannerFormat.LeaderBoard -> type == SlotType.BANNER
                            BannerFormat.Adaptive -> type == SlotType.BANNER
                            BannerFormat.MRec -> type == SlotType.MREC
                        }
                    }?.let {
                        type to it
                    }
                }

                is AdTypeParam.Interstitial -> {
                    slotUuids.takeIf { type == SlotType.INTERSTITIAL || type == SlotType.VIDEO }?.let {
                        type to it
                    }
                }

                is AdTypeParam.Rewarded -> {
                    slotUuids.takeIf { type == SlotType.REWARDED_AD }?.let {
                        type to it
                    }
                }
            }
        }.toMap()
    }

    private fun DTBAdRequest.applyRegulation(regulation: Regulation) {
        regulation.usPrivacyString?.let {
            this.putCustomTarget("us_privacy", it)
        }
    }

    private fun getAmazonSizes(slots: Map<SlotType, List<String>>, adTypeParam: AdTypeParam): List<Pair<SlotType, DTBAdSize>> {
        return slots.mapNotNull { (type, slotUuids) ->
            when (adTypeParam) {
                is AdTypeParam.Banner -> {
                    slotUuids.map { uuid ->
                        type to DTBAdSize(
                            /* width = */ adTypeParam.bannerFormat.width,
                            /* height = */ adTypeParam.bannerFormat.height,
                            /* slotUUID = */ uuid
                        )
                    }
                }

                is AdTypeParam.Interstitial -> {
                    when (type) {
                        SlotType.VIDEO -> getDtbVideoAdList(slotUuids, type)

                        SlotType.INTERSTITIAL -> {
                            slotUuids.map { uuid ->
                                type to DTBAdSize.DTBInterstitialAdSize(uuid)
                            }
                        }

                        else -> null
                    }
                }

                is AdTypeParam.Rewarded -> getDtbVideoAdList(slotUuids, type)
            }
        }.flatten().onEach {
            logInfo(TAG, "AmazonInfo suitable slots ->  ${it.first}: ${it.second.slotUUID}")
        }
    }

    private fun getDtbVideoAdList(
        slotUuids: List<String>,
        type: SlotType
    ): List<Pair<SlotType, DTBAdSize.DTBVideo>> = slotUuids.map { uuid ->
        val playerWidth = DeviceInfo.screenWidthDp.takeIf { it > 0 } ?: 320
        val playerHeight = DeviceInfo.screenHeightDp.takeIf { it > 0 } ?: 480
        logInfo(TAG, "Amazon video player size dp: $playerWidth x $playerHeight")
        type to DTBAdSize.DTBVideo(playerWidth, playerHeight, uuid)
    }
}

private const val TAG = "ObtainTokenUseCase"
