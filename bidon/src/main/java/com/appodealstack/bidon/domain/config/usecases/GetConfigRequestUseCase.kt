package com.appodealstack.bidon.domain.config.usecases

import com.appodealstack.bidon.data.models.config.ConfigRequestBody
import com.appodealstack.bidon.data.models.config.ConfigResponse
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetConfigRequestUseCase {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}
