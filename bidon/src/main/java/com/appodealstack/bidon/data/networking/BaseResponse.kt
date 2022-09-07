package com.appodealstack.bidon.data.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class BaseResponse(
    @SerialName("success")
    val success: Boolean?,
    @SerialName("error")
    val error: Error?,
) {
    @Serializable
    data class Error(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String
    )
}