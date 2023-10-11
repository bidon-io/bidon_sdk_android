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
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import kotlin.coroutines.resume

internal class AmazonInfo(
    val dtbAdResponse: DTBAdResponse,
    val adSizes: DTBAdSize,
)

internal class ObtainTokenUseCase {
    suspend operator fun invoke(slots: Map<SlotType, List<String>>, adTypeParam: AdTypeParam): List<AmazonInfo> {
        return obtainInfo(
            adSizes = getAmazonSizes(slots, adTypeParam)
        )
    }

    private suspend fun obtainInfo(adSizes: List<Pair<SlotType, DTBAdSize>>): List<AmazonInfo> = coroutineScope {
        val results = mutableListOf<AmazonInfo>()
        adSizes
            .map { (_, dtbAdSize) ->
                dtbAdSize to async { getDTBAdResponse(dtbAdSize) }
            }.forEach { (dtbAdSize, deferred) ->
                val dtbAdResponse = deferred.await()
                if (dtbAdResponse != null) {
                    results.add(AmazonInfo(dtbAdResponse, dtbAdSize))
                    logInfo(TAG, "AmazonInfo added-> ${dtbAdSize.slotUUID}: ${dtbAdResponse.getPricePoints(dtbAdSize)}")
                }
            }
        results
    }

    private suspend fun getDTBAdResponse(adSize: DTBAdSize): DTBAdResponse? = suspendCancellableCoroutine { continuation ->
        val loader = DTBAdRequest()
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

    private fun getAmazonSizes(slots: Map<SlotType, List<String>>, adTypeParam: AdTypeParam): List<Pair<SlotType, DTBAdSize>> {
        return slots.mapNotNull { (type, slotUuids) ->
            when (adTypeParam) {
                is AdTypeParam.Banner -> {
                    when (adTypeParam.bannerFormat) {
                        BannerFormat.Banner,
                        BannerFormat.LeaderBoard,
                        BannerFormat.MRec -> {
                            slotUuids.map { uuid ->
                                type to DTBAdSize(
                                    /* width = */ adTypeParam.bannerFormat.getWidthDp(),
                                    /* height = */ adTypeParam.bannerFormat.getHeightDp(),
                                    /* slotUUID = */ uuid
                                )
                            }
                        }

                        BannerFormat.Adaptive -> {
                            if (DeviceInfo.isTablet) {
                                slotUuids.map { uuid ->
                                    type to DTBAdSize(
                                        /* width = */ BannerFormat.Banner.getWidthDp(),
                                        /* height = */ BannerFormat.Banner.getHeightDp(),
                                        /* slotUUID = */ uuid
                                    )
                                }
                            } else {
                                slotUuids.map { uuid ->
                                    type to DTBAdSize(
                                        /* width = */ BannerFormat.LeaderBoard.getWidthDp(),
                                        /* height = */ BannerFormat.LeaderBoard.getHeightDp(),
                                        /* slotUUID = */ uuid
                                    )
                                }
                            }
                        }
                    }
                }

                is AdTypeParam.Interstitial -> {
                    when (type) {
                        SlotType.VIDEO -> {
                            getDtbVideoAdList(slotUuids, type)
                        }

                        SlotType.INTERSTITIAL -> {
                            slotUuids.map { uuid ->
                                type to DTBAdSize.DTBInterstitialAdSize(uuid)
                            }
                        }

                        SlotType.REWARDED_AD,
                        SlotType.BANNER,
                        SlotType.MREC -> {
                            null
                        }
                    }
                }

                is AdTypeParam.Rewarded -> {
                    when (type) {
                        SlotType.REWARDED_AD -> {
                            getDtbVideoAdList(slotUuids, type)
                        }

                        SlotType.VIDEO,
                        SlotType.BANNER,
                        SlotType.MREC,
                        SlotType.INTERSTITIAL -> null
                    }
                }
            }
        }.flatten()
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
