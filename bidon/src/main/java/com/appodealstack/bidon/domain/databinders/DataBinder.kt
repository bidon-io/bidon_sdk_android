package com.appodealstack.bidon.domain.databinders

import kotlinx.serialization.json.JsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 *  @see Scheme[https://appodeal.atlassian.net/wiki/spaces/SX/pages/4490264831/SDK+Server+Schema#SDK%3C%3EServerSchema-Session]
 *
 *  @see [DataBinderType] List of Binders
 */
internal interface DataBinder {
    val fieldName: String
    suspend fun getJsonElement(): JsonElement
}
