package org.bidon.bidmachine.impl

import io.bidmachine.CustomParams
import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class GetAdAuctionParamUseCase {
    fun getBMFullscreenAuctionParams(auctionParamsScope: AdAuctionParamSource): Result<BMFullscreenAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = adUnit.pricefloor,
                timeout = adUnit.timeout,
                context = activity.applicationContext,
                adUnit = adUnit,
                payload = adUnit.extra?.optString("payload"),
                placement = adUnit.extra?.optString("placement"),
                customParameters = buildCustomParameters(adUnit.extra)
            )
        }
    }

    fun getBMBannerAuctionParams(auctionParamsScope: AdAuctionParamSource): Result<BMBannerAuctionParams> {
        return auctionParamsScope {
            BMBannerAuctionParams(
                price = adUnit.pricefloor,
                timeout = adUnit.timeout,
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit,
                payload = adUnit.extra?.optString("payload"),
                placement = adUnit.extra?.optString("placement"),
                customParameters = buildCustomParameters(adUnit.extra),
            )
        }
    }

    private fun buildCustomParameters(extra: JSONObject?): CustomParams {
        val customParams = CustomParams().addParam("mediation_mode", "bidon")
        val paramsJson = extra?.optJSONObject("custom_parameters") ?: return customParams

        try {
            val keys = paramsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = paramsJson.optString(key)
                customParams.addParam(key, value)
            }
        } catch (_: Throwable) {
            // ignore
        }

        return customParams
    }
}