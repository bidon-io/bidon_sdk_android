package org.bidon.sdk.config.usecases

import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.models.ConfigResponse

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface GetConfigRequestUseCase {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}
