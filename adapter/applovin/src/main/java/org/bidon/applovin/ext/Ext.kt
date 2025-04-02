package org.bidon.applovin.ext

import com.applovin.sdk.AppLovinSdk
import org.bidon.applovin.ApplovinDemandId
import org.bidon.applovin.BuildConfig
import org.bidon.sdk.config.BidonError

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = AppLovinSdk.VERSION

internal fun Int.asBidonError() =
    when (this) {
        204 -> BidonError.NoFill(ApplovinDemandId)
        else -> BidonError.Unspecified(ApplovinDemandId, Throwable("Code: $this"))
    }
