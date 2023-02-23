package org.bidon.ironsource.ext

import com.ironsource.mediationsdk.utils.IronSourceUtils
import org.bidon.ironsource.BuildConfig

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = IronSourceUtils.getSDKVersion()
