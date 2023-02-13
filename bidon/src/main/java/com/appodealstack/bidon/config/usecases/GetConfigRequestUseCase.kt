package com.appodealstack.bidon.config.usecases

import com.appodealstack.bidon.config.models.ConfigRequestBody
import com.appodealstack.bidon.config.models.ConfigResponse

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetConfigRequestUseCase {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}
