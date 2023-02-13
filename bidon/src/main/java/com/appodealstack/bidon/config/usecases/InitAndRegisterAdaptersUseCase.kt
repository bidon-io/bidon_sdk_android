package com.appodealstack.bidon.config.usecases

import android.app.Activity
import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.config.models.ConfigResponse

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface InitAndRegisterAdaptersUseCase {
    suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        publisherAdapters: List<Adapter>,
        configResponse: ConfigResponse
    )
}
