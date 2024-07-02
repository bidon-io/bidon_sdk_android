package org.bidon.amazon.impl

import org.bidon.amazon.SlotType
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal class ParseSlotsUseCase {
    operator fun invoke(jsonObject: JSONObject): Map<SlotType, List<String>> = buildMap {
        jsonObject.getJSONArray("slots").let { slots ->
            repeat(slots.length()) { index ->
                runCatching {
                    val slot = slots.getJSONObject(index)
                    val format = slot.getString("format")
                    val adType = SlotType.getOrNull(format) ?: error("Unknown slot type $format")
                    val slotUuid = slot.getString("slot_uuid")
                    put(adType, (get(adType) ?: emptyList()) + slotUuid)
                }
            }
        }
    }
}