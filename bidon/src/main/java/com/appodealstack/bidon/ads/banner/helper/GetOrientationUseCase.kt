package com.appodealstack.bidon.ads.banner.helper

import com.appodealstack.bidon.auction.models.AdObjectRequestBody
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetOrientationUseCase {
    operator fun invoke(): AdObjectRequestBody.Orientation
}