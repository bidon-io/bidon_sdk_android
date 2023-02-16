package com.appodealstack.bidon.utils.networking

import com.appodealstack.bidon.utils.json.JsonParser
import com.appodealstack.bidon.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class BaseResponse(
    val success: Boolean?,
    val error: Error?,
) {
    data class Error(
        val code: Int,
        val message: String,
    )
}

internal class BaseResponseParser : JsonParser<BaseResponse> {
    override fun parseOrNull(jsonString: String): BaseResponse? = runCatching {
        JSONObject(jsonString).let { json ->
            BaseResponse(
                success = json.optBoolean("success"),
                error = JsonParsers.parseOrNull(json.optString("error"))
            )
        }
    }.getOrNull()
}

internal class BaseResponseErrorParser : JsonParser<BaseResponse.Error> {
    override fun parseOrNull(jsonString: String): BaseResponse.Error? = runCatching {
        JSONObject(jsonString).let { json ->
            BaseResponse.Error(
                code = json.getInt("code"),
                message = json.getString("message")
            )
        }
    }.getOrNull()
}