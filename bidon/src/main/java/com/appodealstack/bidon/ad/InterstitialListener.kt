package com.appodealstack.bidon.ad

import com.appodealstack.bidon.adapters.NewAdListener
import com.appodealstack.bidon.auctions.domain.NewAuctionListener
import com.appodealstack.bidon.auctions.domain.RoundsListener

interface InterstitialListener : NewAdListener, NewAuctionListener, RoundsListener