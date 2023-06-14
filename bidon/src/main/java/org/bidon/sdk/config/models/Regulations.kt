package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class Regulations(
    @field:JsonName("coppa")
    val coppa: Boolean,
    @field:JsonName("gdpr")
    val gdpr: Boolean,
) : Serializable
