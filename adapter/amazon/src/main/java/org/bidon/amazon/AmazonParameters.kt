package org.bidon.amazon

import org.bidon.sdk.adapter.AdapterParameters

internal class AmazonParameters(
    val appKey: String,
    val slots: Map<SlotType, List<String>>
) : AdapterParameters
