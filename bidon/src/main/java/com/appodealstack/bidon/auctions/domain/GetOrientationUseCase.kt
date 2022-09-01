package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody

internal interface GetOrientationUseCase {
    operator fun invoke(): AdObjectRequestBody.Orientation
}