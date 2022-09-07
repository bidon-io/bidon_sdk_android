package com.appodealstack.bidon.view.helper

import com.appodealstack.bidon.data.models.auction.AdObjectRequestBody

internal interface GetOrientationUseCase {
    operator fun invoke(): AdObjectRequestBody.Orientation
}