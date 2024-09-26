package org.bidon.amazon

import com.amazon.device.ads.AdError
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.amazon.device.ads.SDKUtilities
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.Queue
import kotlin.coroutines.resume

/**
 * Singleton instance of AmazonBidManager.
 */
internal val amazonBidManager: AmazonBidManager by lazy { AmazonBidManager() }

/**
 * Manages Amazon ad tokens and responses.
 */
internal class AmazonBidManager {

    /**
     * Stores slotUUIDs as keys and their corresponding queues (Queue<DTBAdResponse>) as values.
     *
     * - `mutableMapOf` allows dynamic addition or update of values (queues of responses) associated with each unique slotUUID.
     * - `Queue` implements FIFO (First In, First Out), ideal for processing multiple `DTBAdResponse` objects in the order they are received.
     * - `LinkedList` is used as the `Queue` implementation for efficient handling of adding and removing elements.
     */
    private val dtbAdResponses = mutableMapOf<String, Queue<DTBAdResponse>>()

    /**
     * Retrieves the current regulation settings from BidonSdk.
     */
    private val regulation: Regulation
        get() = BidonSdk.regulation

    /**
     * Obtains an ad token based on the provided slots and ad type parameter.
     *
     * @param slots A map of SlotType to a list of slot UUIDs.
     * @param adTypeParam The ad type parameter specifying the type of ad.
     * @return A JSON string representing the ad token, or null if no suitable slot is found.
     */
    suspend fun obtainToken(slots: Map<SlotType, List<String>>, adTypeParam: AdTypeParam): String? {
        val filteredSlots = slots.filterBy(adTypeParam)
        val amazonInfo = obtainInfo(getDtbAdSizes(filteredSlots, adTypeParam))

        if (amazonInfo.isEmpty()) return null

        amazonInfo.forEach { (slotUuid, dtbAdResponse) ->
            dtbAdResponses.getOrPut(slotUuid) { LinkedList() }.add(dtbAdResponse)
        }

        return JSONArray().apply {
            amazonInfo.forEach { (slotUuid, dtbAdResponse) ->
                put(JSONObject().apply {
                    put("slot_uuid", slotUuid)
                    put("price_point", SDKUtilities.getPricePoint(dtbAdResponse))
                })
            }
        }.toString()
    }

    /**
     * Retrieves the next ad response for the given slot UUID.
     *
     * @param slotUuid The UUID of the slot.
     * @return The next DTBAdResponse for the slot, or null if no response is available.
     */
    fun getResponse(slotUuid: String): DTBAdResponse? {
        return dtbAdResponses[slotUuid]?.poll()
    }

    /**
     * Obtains information about the ad sizes asynchronously.
     *
     * @param adSizes A list of DTBAdSize objects.
     * @return A list of pairs containing slot UUIDs and their corresponding DTBAdResponse objects.
     */
    private suspend fun obtainInfo(adSizes: List<DTBAdSize>): List<Pair<String, DTBAdResponse>> = coroutineScope {
        adSizes.map { dtbAdSize ->
            logInfo(TAG, "AmazonInfo request -> ${dtbAdSize.dtbAdType}: ${dtbAdSize.slotUUID}")
            async {
                getDTBAdResponse(dtbAdSize)?.let { dtbAdResponse ->
                    logInfo(TAG, "AmazonInfo response -> ${dtbAdSize.dtbAdType}: ${dtbAdSize.slotUUID}, $dtbAdResponse")
                    dtbAdSize.slotUUID to dtbAdResponse
                }
            }
        }.awaitAll().filterNotNull()
    }

    /**
     * Retrieves a DTBAdResponse for the given ad size.
     *
     * @param adSize The DTBAdSize object.
     * @return The DTBAdResponse object, or null if the request fails.
     */
    private suspend fun getDTBAdResponse(adSize: DTBAdSize): DTBAdResponse? = suspendCancellableCoroutine { continuation ->
        val loader = DTBAdRequest()
        loader.applyRegulation(regulation)
        loader.setSizes(adSize)
        loader.loadAd(object : DTBAdCallback {
            override fun onFailure(adError: AdError) {
                logError(TAG, "Error while loading ad: $adSize ${adError.code} ${adError.message}", BidonError.NoBid)
                continuation.resume(null)
            }

            override fun onSuccess(dtbAdResponse: DTBAdResponse) {
                continuation.resume(dtbAdResponse)
            }
        })
    }

    /**
     * Applies regulation settings to the DTBAdRequest.
     *
     * @param regulation The Regulation object containing the settings.
     */
    private fun DTBAdRequest.applyRegulation(regulation: Regulation) {
        regulation.usPrivacyString?.let {
            this.putCustomTarget("us_privacy", it)
        }
    }

    /**
     * Filters the slots based on the ad type parameter.
     *
     * @param adTypeParam The ad type parameter specifying the type of ad.
     * @return A filtered map of SlotType to a list of slot UUIDs.
     */
    private fun Map<SlotType, List<String>>.filterBy(adTypeParam: AdTypeParam): Map<SlotType, List<String>> {
        return this.filter { (type, _) ->
            when (adTypeParam) {
                is AdTypeParam.Banner -> {
                    type == SlotType.BANNER || (adTypeParam.bannerFormat == BannerFormat.MRec && type == SlotType.MREC)
                }
                is AdTypeParam.Interstitial -> {
                    type == SlotType.INTERSTITIAL || type == SlotType.VIDEO
                }
                is AdTypeParam.Rewarded -> {
                    type == SlotType.REWARDED_AD
                }
            }
        }
    }

    /**
     * Retrieves a list of DTBAdSize objects based on the provided slots and ad type parameter.
     *
     * @param slots A map of SlotType to a list of slot UUIDs.
     * @param adTypeParam The ad type parameter specifying the type of ad.
     * @return A list of DTBAdSize objects.
     */
    private fun getDtbAdSizes(
        slots: Map<SlotType, List<String>>,
        adTypeParam: AdTypeParam
    ): List<DTBAdSize> {
        return slots.flatMap { (type, slotUuids) ->
            slotUuids.mapNotNull { uuid ->
                when (adTypeParam) {
                    is AdTypeParam.Banner -> {
                        DTBAdSize(adTypeParam.bannerFormat.width, adTypeParam.bannerFormat.height, uuid)
                    }
                    is AdTypeParam.Interstitial -> {
                        if (type == SlotType.VIDEO) getDtbVideoAdSize(uuid) else DTBAdSize.DTBInterstitialAdSize(uuid)
                    }
                    is AdTypeParam.Rewarded -> {
                        getDtbVideoAdSize(uuid)
                    }
                    else -> {
                        null
                    }
                }
            }
        }.onEach {
            logInfo(TAG, "AmazonInfo suitable slot UUID -> ${it.slotUUID}")
        }
    }

    /**
     * Retrieves a DTBVideo ad size based on the device's screen dimensions.
     *
     * @param uuid The UUID of the slot.
     * @return The DTBVideo ad size.
     */
    private fun getDtbVideoAdSize(uuid: String): DTBAdSize.DTBVideo {
        val playerWidth = DeviceInfo.screenWidthDp.takeIf { it > 0 } ?: 320
        val playerHeight = DeviceInfo.screenHeightDp.takeIf { it > 0 } ?: 480
        logInfo(TAG, "Amazon video player size dp: $playerWidth x $playerHeight")
        return DTBAdSize.DTBVideo(playerWidth, playerHeight, uuid)
    }
}

private const val TAG = "AmazonBidManager"