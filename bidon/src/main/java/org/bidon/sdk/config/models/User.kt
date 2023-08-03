package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class User(
    @field:JsonName("idfa")
    var platformAdvertisingId: String, // idfa = iOS, AD_ID - Android.
    @field:JsonName("tracking_authorization_status")
    var trackingAuthorizationStatus: String,
    @field:JsonName("idg")
    var applicationId: String?, // ID that app generates on the very first launch and send across session.
) : Serializable
