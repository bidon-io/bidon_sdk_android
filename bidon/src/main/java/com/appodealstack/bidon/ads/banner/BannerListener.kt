package com.appodealstack.bidon.ads.banner

import com.appodealstack.bidon.ads.AdListener
import com.appodealstack.bidon.auction.AuctionListener
import com.appodealstack.bidon.auction.RoundsListener
import com.appodealstack.bidon.logs.analytic.AdRevenueListener

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface BannerListener :
    AdListener,
    AdRevenueListener,
    AuctionListener,
    RoundsListener
