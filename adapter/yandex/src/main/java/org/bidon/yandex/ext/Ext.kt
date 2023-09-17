package org.bidon.yandex.ext

import com.yandex.mobile.ads.common.MobileAds
import org.bidon.yandex.BuildConfig

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.getLibraryVersion()
