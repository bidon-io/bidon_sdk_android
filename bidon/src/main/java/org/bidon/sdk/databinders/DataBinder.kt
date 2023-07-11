package org.bidon.sdk.databinders

/**
 * Created by Bidon Team on 06/02/2023.
 *
 *  @see [DataBinderType] List of Binders
 */
internal interface DataBinder<JsonElement> {
    val fieldName: String
    suspend fun getJsonObject(): JsonElement?
}
