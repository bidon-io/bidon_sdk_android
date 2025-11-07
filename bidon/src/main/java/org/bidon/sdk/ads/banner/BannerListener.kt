package org.bidon.sdk.ads.banner

import org.bidon.sdk.ads.AdListener
import org.bidon.sdk.logs.analytic.AdRevenueListener

/**
 * Created by Bidon Team on 06/02/2023.
 */
public interface BannerListener :
    AdListener,
    AdRevenueListener
