package org.bidon.admob.ext

import com.google.android.gms.ads.MobileAds
import org.bidon.admob.BuildConfig

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.getVersion().toString()
