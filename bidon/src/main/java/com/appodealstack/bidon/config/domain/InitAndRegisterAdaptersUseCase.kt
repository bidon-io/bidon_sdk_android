package com.appodealstack.bidon.config.domain

import android.app.Activity
import com.appodealstack.bidon.adapters.Adapter
import com.appodealstack.bidon.config.data.models.ConfigResponse

internal interface InitAndRegisterAdaptersUseCase {
    suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        configResponse: ConfigResponse
    )
}