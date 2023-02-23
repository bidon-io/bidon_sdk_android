package org.bidon.sdk.ads.banner.helper

import org.bidon.sdk.auction.models.AdObjectRequestBody
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetOrientationUseCase {
    operator fun invoke(): AdObjectRequestBody.Orientation
}