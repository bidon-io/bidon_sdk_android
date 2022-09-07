package com.appodealstack.bidon.domain.config.usecases

import android.app.Activity
import com.appodealstack.bidon.data.models.config.ConfigResponse
import com.appodealstack.bidon.domain.adapter.Adapter

internal interface InitAndRegisterAdaptersUseCase {
    suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        configResponse: ConfigResponse
    )
}
