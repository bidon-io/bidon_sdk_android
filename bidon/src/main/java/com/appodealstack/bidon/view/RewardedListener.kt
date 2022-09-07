package com.appodealstack.bidon.view

import com.appodealstack.bidon.domain.auction.AuctionListener
import com.appodealstack.bidon.domain.auction.RoundsListener
import com.appodealstack.bidon.domain.common.AdListener
import com.appodealstack.bidon.domain.common.RewardedAdListener

interface RewardedListener : AdListener, AuctionListener, RoundsListener, RewardedAdListener
