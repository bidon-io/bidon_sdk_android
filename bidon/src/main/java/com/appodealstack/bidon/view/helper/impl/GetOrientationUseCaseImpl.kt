package com.appodealstack.bidon.view.helper.impl

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.appodealstack.bidon.data.models.auction.AdObjectRequestBody.Orientation
import com.appodealstack.bidon.view.helper.GetOrientationUseCase
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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