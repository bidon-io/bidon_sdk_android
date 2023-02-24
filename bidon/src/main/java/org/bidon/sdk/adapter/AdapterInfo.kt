package org.bidon.sdk.adapter

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class AdapterInfo(
    @field:JsonName("version")
    val adapterVersion: String,
    @field:JsonName("sdk_version")
    val sdkVersion: String
) : Serializable