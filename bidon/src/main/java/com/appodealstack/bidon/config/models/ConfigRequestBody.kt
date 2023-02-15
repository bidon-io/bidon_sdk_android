package com.appodealstack.bidon.config.models

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Map< AdapterName:String, AdapterInfo >
 */
internal data class ConfigRequestBody(
    val adapters: Map<String, AdapterInfo>
)