package org.bidon.sdk.config.impl

import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/03/2023.
 */
object ServerlessConfigSettings {

    private var adaptersToInit = mapOf(
        DefaultAdapters.AdmobAdapter to true,
        DefaultAdapters.BidmachineAdapter to true,
        DefaultAdapters.ApplovinAdapter to true,
        DefaultAdapters.DTExchangeAdapter to true,
        DefaultAdapters.UnityAdsAdapter to true,
    )

    fun useAdapters(vararg adapters: String) {
        adaptersToInit = adapters.associate { adapterName ->
            when (adapterName) {
                "admob" -> DefaultAdapters.AdmobAdapter
                "applovin" -> DefaultAdapters.ApplovinAdapter
                "bidmachine" -> DefaultAdapters.BidmachineAdapter
                "dtexchange" -> DefaultAdapters.DTExchangeAdapter
                "unityads" -> DefaultAdapters.UnityAdsAdapter
                else -> error("Unknown adapter")
            } to true
        }
    }

    internal fun getConfigResponse(): ConfigResponse {
        return ConfigResponse(
            initializationTimeout = 15000,
            adapters = adaptersToInit.mapNotNull { (defaultAdapter, shouldInitialize) ->
                if (shouldInitialize) {
                    when (defaultAdapter) {
                        DefaultAdapters.AdmobAdapter -> "admob" to admobOptions
                        DefaultAdapters.BidmachineAdapter -> "bidmachine" to bidmachineOptions
                        DefaultAdapters.ApplovinAdapter -> "applovin" to jsonObject { }
                        DefaultAdapters.DTExchangeAdapter -> "dtexchange" to dtexchangeOptions
                        DefaultAdapters.UnityAdsAdapter -> "unityads" to unityadsOptions
                        DefaultAdapters.BigoAdsAdapter -> TODO()
                        DefaultAdapters.MintegralAdapter -> TODO()
                        DefaultAdapters.VungleAdapter -> TODO()
                        DefaultAdapters.MetaAdapter -> TODO()
                        DefaultAdapters.InmobiAdapter -> TODO()
                    }
                } else {
                    null
                }
            }.toMap()
        )
    }

    private val admobOptions = jsonObject {}
    private val bidmachineOptions = jsonObject {}

    private val dtexchangeOptions = JSONObject("""{"app_id": "102960"}""")
    private val unityadsOptions = JSONObject("""{"game_id": "5186538"}""")
}
