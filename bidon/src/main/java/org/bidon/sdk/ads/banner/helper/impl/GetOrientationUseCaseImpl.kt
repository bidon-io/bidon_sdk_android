package org.bidon.sdk.ads.banner.helper.impl

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import org.bidon.sdk.ads.banner.helper.GetOrientationUseCase
import org.bidon.sdk.auction.models.AdObjectRequest.Orientation
/**
 * Created by Bidon Team on 06/02/2023.
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