package org.bidon.sdk.config.impl

import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.asSuccess

/**
 * Created by Aleksei Cherniaev on 06/03/2023.
 */
internal class GetConfigRequestUseCaseImpl : GetConfigRequestUseCase {
    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        logInfo("GetConfigRequestUseCase", "----------------------------- SERVERLESS DATA / USE ONLY FOR TEST ----------------------------- ")
        return ServerlessConfigSettings.getConfigResponse().asSuccess()
    }
}