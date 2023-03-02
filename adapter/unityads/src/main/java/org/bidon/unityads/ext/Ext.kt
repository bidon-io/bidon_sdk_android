package org.bidon.unityads.ext

import com.unity3d.ads.UnityAds
import org.bidon.unityads.BuildConfig

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = UnityAds.getVersion()
