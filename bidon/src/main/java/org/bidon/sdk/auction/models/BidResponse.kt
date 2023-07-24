package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class BidResponse(
    @field:JsonName("bids")
    val bids: List<Bid>?,
    @field:JsonName("status")
    val status: BidStatus,
) : Serializable {
    enum class BidStatus(val code: String) {
        Success("SUCCESS"),
        NoBid("NO_BID");

        companion object {
            fun get(code: String) = values().first { it.code == code }
        }
    }
}

internal class BidResponseParser : JsonParser<BidResponse> {
    override fun parseOrNull(jsonString: String): BidResponse? = runCatching {
        val json = JSONObject(jsonString)
        BidResponse(
            bids = json.optJSONArray("bids")?.let { array ->
                buildList {
                    repeat(array.length()) { index ->
                        array.optJSONObject(index)
                            ?.let { bidJson ->
                                val bid = Bid(
                                    id = bidJson.getString("id"),
                                    impressionId = bidJson.optString("impid"),
                                    price = bidJson.getDouble("price"),
                                    demands = bidJson.optJSONObject("demands")?.parseDemands() ?: emptyList()
                                )
                                add(bid)
                            }
                    }
                }
            },
            status = json.getString("status").let {
                BidResponse.BidStatus.get(it)
            }
        )
    }.getOrNull()

    private fun JSONObject.parseDemands(): List<BidDemand> = buildList {
        this@parseDemands.keys().forEach { demandId ->
            runCatching {
                val json = this@parseDemands.getJSONObject(demandId)
                when (BidDemandName.getOrNull(demandId)) {
                    BidDemandName.Mintegral -> {
                        BidDemand.Mintegral(
                            payload = json.getString("payload"),
                            unitId = json.getString("unit_id"),
                            placementId = json.getString("placement_id")
                        )
                    }

                    BidDemandName.BidMachine -> {
                        BidDemand.BidMachine(
                            payload = json.getString("payload")
                        )
                    }

                    BidDemandName.Mobilefuse -> {
                        BidDemand.Mobilefuse(
                            payload = json.getString("payload"),
                            placementId = json.getString("placement_id")
                        )
                    }

                    BidDemandName.Vungle -> {
                        BidDemand.Vungle(
                            payload = json.getString("payload"),
                            placementId = json.getString("placement_id")
                        )
                    }

                    BidDemandName.BigoAds -> {
                        BidDemand.BigoAds(
                            payload = json.getString("payload"),
                            slotId = json.getString("slot_id")
                        )
                    }

                    BidDemandName.Meta -> {
                        BidDemand.Meta(
                            payload = json.getString("payload"),
                            placementId = json.getString("placement_id")
                        )
                    }

                    null -> error("Unknown demandId: $demandId")
                }
            }.getOrNull()?.let(::add)
        }
    }
}