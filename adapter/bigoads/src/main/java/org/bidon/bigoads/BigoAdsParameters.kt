package org.bidon.bigoads

import org.bidon.sdk.adapter.AdapterParameters

internal data class BigoAdsParameters(
    val appId: String,
    val channel: String?,
) : AdapterParameters
