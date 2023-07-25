package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */

internal enum class BidDemandName(val code: String) {
    Mintegral("mintegral"),
    BidMachine("bidmachine"),
    Mobilefuse("mobilefuse"),
    Vungle("vungle"),
    BigoAds("bigoads"),
    Meta("meta");

    companion object {
        fun getOrNull(key: String) = values().firstOrNull { it.code == key }
    }
}

internal sealed interface BidDemand {
    val payload: String
    val id: BidDemandName

    data class Mintegral(
        @field:JsonName("payload")
        override val payload: String,
        @field:JsonName("unit_id")
        val unitId: String,
        @field:JsonName("placement_id")
        val placementId: String
    ) : BidDemand {
        override val id = BidDemandName.Mintegral
    }

    data class BidMachine(
        @field:JsonName("payload")
        override val payload: String,
    ) : BidDemand {
        override val id = BidDemandName.BidMachine
        override fun toString(): String {
            return "BidMachine(payload=${payload.take(4)}..${payload.takeLast(4)})"
        }
    }

    data class Mobilefuse(
        @field:JsonName("payload")
        override val payload: String,
        @field:JsonName("placement_id")
        val placementId: String
    ) : BidDemand {
        override val id = BidDemandName.Mobilefuse
    }

    data class Vungle(
        @field:JsonName("payload")
        override val payload: String,
        @field:JsonName("placement_id")
        val placementId: String
    ) : BidDemand {
        override val id = BidDemandName.Vungle
    }

    data class BigoAds(
        @field:JsonName("payload")
        override val payload: String,
        @field:JsonName("slot_id")
        val slotId: String
    ) : BidDemand {
        override val id = BidDemandName.BigoAds
    }

    data class Meta(
        @field:JsonName("payload")
        override val payload: String,
        @field:JsonName("placement_id")
        val placementId: String
    ) : BidDemand {
        override val id = BidDemandName.Meta
    }
}