package com.appodealstack.admob.ext

import com.appodealstack.admob.BuildConfig
import com.google.android.gms.ads.MobileAds

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.getVersion().toString()