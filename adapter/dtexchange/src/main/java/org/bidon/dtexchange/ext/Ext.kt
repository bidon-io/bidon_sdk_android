package org.bidon.dtexchange.ext

import com.fyber.inneractive.sdk.external.InneractiveAdManager
import org.bidon.dtexchange.BuildConfig

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = InneractiveAdManager.getVersion()
