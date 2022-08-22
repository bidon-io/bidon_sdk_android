package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse

internal interface GetConfigRequestUseCase {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}
