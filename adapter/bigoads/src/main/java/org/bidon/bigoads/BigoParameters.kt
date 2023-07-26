package org.bidon.bigoads

import org.bidon.sdk.adapter.AdapterParameters

data class BigoParameters(
    val appId: String,
    val channel: String,
) : AdapterParameters
