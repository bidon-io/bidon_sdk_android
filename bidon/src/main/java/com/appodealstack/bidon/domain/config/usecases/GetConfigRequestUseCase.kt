package com.appodealstack.bidon.domain.config.usecases

import com.appodealstack.bidon.data.models.config.ConfigRequestBody
import com.appodealstack.bidon.data.models.config.ConfigResponse

internal interface GetConfigRequestUseCase {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}
