package com.appodealstack.bidon.config.domain

import android.app.Activity
import com.appodealstack.bidon.demands.Adapter

internal interface InitAndRegisterAdaptersUseCase {
    suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        configResponse: ConfigResponse
    )
}