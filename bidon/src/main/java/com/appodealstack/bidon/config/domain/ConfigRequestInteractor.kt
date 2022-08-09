package com.appodealstack.bidon.config.domain

internal interface ConfigRequestInteractor {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}