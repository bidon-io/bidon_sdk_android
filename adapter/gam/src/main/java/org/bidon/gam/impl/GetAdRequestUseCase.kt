package org.bidon.gam.impl

import com.google.android.gms.ads.admanager.AdManagerAdRequest

internal class GetAdRequestUseCase {
    operator fun invoke() = AdManagerAdRequest.Builder()
        .build()
}