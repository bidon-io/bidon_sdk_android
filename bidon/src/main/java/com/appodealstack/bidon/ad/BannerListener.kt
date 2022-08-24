package com.appodealstack.bidon.ad

import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.auctions.domain.AuctionListener
import com.appodealstack.bidon.auctions.domain.RoundsListener

interface BannerListener : AdListener, AuctionListener, RoundsListener
