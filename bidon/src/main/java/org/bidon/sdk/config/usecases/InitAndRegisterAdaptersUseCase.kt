package org.bidon.sdk.config.usecases

import android.content.Context
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.models.ConfigResponse

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface InitAndRegisterAdaptersUseCase {
    suspend operator fun invoke(
        context: Context,
        adapters: List<Adapter>,
        configResponse: ConfigResponse,
        isTestMode: Boolean
    )
}
