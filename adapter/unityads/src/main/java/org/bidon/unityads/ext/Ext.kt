package org.bidon.unityads.ext

import com.unity3d.services.ads.UnityAdsImplementation
import org.bidon.unityads.BuildConfig

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = UnityAdsImplementation.getInstance().getVersion()
