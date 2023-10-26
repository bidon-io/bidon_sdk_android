package org.bidon.amazon.ext

import com.amazon.device.ads.AdRegistration
import org.bidon.amazon.BuildConfig

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = AdRegistration.getVersion()
