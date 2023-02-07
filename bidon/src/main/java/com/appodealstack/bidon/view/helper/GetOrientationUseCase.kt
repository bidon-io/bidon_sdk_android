package com.appodealstack.bidon.view.helper

import com.appodealstack.bidon.data.models.auction.AdObjectRequestBody
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetOrientationUseCase {
    operator fun invoke(): AdObjectRequestBody.Orientation
}