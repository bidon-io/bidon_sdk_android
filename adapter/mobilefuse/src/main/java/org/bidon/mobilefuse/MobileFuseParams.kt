package org.bidon.mobilefuse

import org.bidon.sdk.adapter.AdapterParameters

data class MobileFuseParams(
    val publisherId: Int,
    val appId: Int
) : AdapterParameters
