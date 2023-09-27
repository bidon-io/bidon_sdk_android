package org.bidon.mobilefuse.ext

import com.mobilefuse.sdk.MobileFuse
import org.bidon.mobilefuse.BuildConfig

/**
 * Created by Aleksei Cherniaev on 06/07/2023.
 */
internal const val adapterVersion = BuildConfig.ADAPTER_VERSION
internal val sdkVersion = MobileFuse.getSdkVersion()