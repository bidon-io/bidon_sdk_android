package org.bidon.sdk.config.models

import org.bidon.sdk.adapter.AdapterInfo

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Map< AdapterName:String, AdapterInfo >
 */
internal data class ConfigRequestBody(
    val adapters: Map<String, AdapterInfo>
)
