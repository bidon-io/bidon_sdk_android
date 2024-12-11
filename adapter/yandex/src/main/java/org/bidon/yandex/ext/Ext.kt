package org.bidon.yandex.ext

import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.yandex.BuildConfig
import org.bidon.yandex.YandexDemandId
import org.json.JSONObject

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.libraryVersion

internal fun AdRequestError?.asBidonError() = when (this?.code) {
    AdRequestError.Code.INTERNAL_ERROR -> BidonError.Unspecified(YandexDemandId)
    AdRequestError.Code.INVALID_REQUEST -> BidonError.IncorrectAdUnit(YandexDemandId, "Invalid request")
    AdRequestError.Code.NETWORK_ERROR -> BidonError.NetworkError(YandexDemandId, description)
    AdRequestError.Code.NO_FILL -> BidonError.NoFill(YandexDemandId)
    AdRequestError.Code.SYSTEM_ERROR -> BidonError.Unspecified(YandexDemandId)
    AdRequestError.Code.UNKNOWN_ERROR -> BidonError.Unspecified(YandexDemandId)
    else -> BidonError.Unspecified(YandexDemandId)
}

internal fun ImpressionData?.asBidonAdValue(): AdValue {
    val revenue: Double? = try {
        val jsonData = this?.rawData
        if (jsonData.isNullOrEmpty()) {
            null
        } else {
            JSONObject(jsonData).optString("revenueUSD").toDoubleOrNull()
        }
    } catch (e: Exception) {
        null
    }
    return AdValue(
        adRevenue = revenue ?: 0.0,
        currency = AdValue.USD,
        precision = Precision.Precise
    )
}
