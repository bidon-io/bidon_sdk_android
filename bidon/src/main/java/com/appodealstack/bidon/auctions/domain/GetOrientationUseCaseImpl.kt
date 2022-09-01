package com.appodealstack.bidon.auctions.domain

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody.Orientation

internal class GetOrientationUseCaseImpl(
    private val context: Context
) : GetOrientationUseCase {
    override fun invoke(): Orientation {
        return when (context.resources.configuration.orientation) {
            ORIENTATION_PORTRAIT -> Orientation.Portrait
            ORIENTATION_LANDSCAPE -> Orientation.Landscape
            else -> Orientation.Portrait
        }
    }
}