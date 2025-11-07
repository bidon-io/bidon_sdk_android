package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 14/11/2023.
 */
public data class TokenInfo(
    @field:JsonName("token")
    val token: String?,
    @field:JsonName("token_start_ts")
    val tokenStartTs: Long?,
    @field:JsonName("token_finish_ts")
    val tokenFinishTs: Long?,
    @field:JsonName("status")
    val status: String,
) : Serializable {
    public enum class Status(internal val code: String) {
        SUCCESS("SUCCESS"),
        TIMEOUT_REACHED("TIMEOUT_REACHED"),
        NO_TOKEN("NO_TOKEN"),
    }
}
