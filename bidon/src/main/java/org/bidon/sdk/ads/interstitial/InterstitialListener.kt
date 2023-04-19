package org.bidon.sdk.ads.interstitial

import org.bidon.sdk.ads.AdListener
import org.bidon.sdk.ads.FullscreenAdListener
import org.bidon.sdk.logs.analytic.AdRevenueListener

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface InterstitialListener :
    AdListener,
    AdRevenueListener,
    FullscreenAdListener
