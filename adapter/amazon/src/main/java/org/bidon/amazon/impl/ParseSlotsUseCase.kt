package org.bidon.amazon.impl

import org.bidon.amazon.SlotType
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal class ParseSlotsUseCase {
    operator fun invoke(adUnits: List<AdUnit>): Map<SlotType, List<String>> = buildMap {
        adUnits.mapNotNull {
            it.extra
        }.map { slot ->
            runCatching {
                val format = slot.getString("format")
                val adType = SlotType.getOrNull(format) ?: error("Unknown slot type $format")
                val slotUuid = slot.getString("slot_uuid")
                put(adType, (get(adType) ?: emptyList()) + slotUuid)
            }
        }
    }
}