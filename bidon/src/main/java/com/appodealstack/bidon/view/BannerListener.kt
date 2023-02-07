package com.appodealstack.bidon.view

import com.appodealstack.bidon.domain.auction.AuctionListener
import com.appodealstack.bidon.domain.auction.RoundsListener
import com.appodealstack.bidon.domain.common.AdListener
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface BannerListener : AdListener, AuctionListener, RoundsListener
