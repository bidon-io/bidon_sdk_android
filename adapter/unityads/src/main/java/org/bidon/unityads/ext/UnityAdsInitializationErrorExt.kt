package org.bidon.unityads.ext

import com.unity3d.ads.UnityAds
import org.bidon.sdk.config.BidonError
import org.bidon.unityads.UnityAdsDemandId

/**
 * Created by Aleksei Cherniaev on 01/03/2023.
 */
internal fun UnityAds.UnityAdsInitializationError?.asBidonError() =
    BidonError.Unspecified(UnityAdsDemandId, Throwable("UnityAdsInitializationError: $this"))
