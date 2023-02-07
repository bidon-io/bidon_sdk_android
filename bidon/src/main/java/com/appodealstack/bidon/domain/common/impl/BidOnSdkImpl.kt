package com.appodealstack.bidon.domain.common.impl

import com.appodealstack.bidon.BidOnBuilder
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import com.appodealstack.bidon.domain.analytic.AdRevenueLogger
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.impl.BidOnInitializerImpl

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOnSdkImpl(private val bidOnInitializer: BidOnInitializer = BidOnInitializerImpl()) :
    BidOnSdk,
    BidOnBuilder by bidOnInitializer {

    private val adaptersSource: AdaptersSource by lazy { get() }

    override fun isInitialized(): Boolean {
        return bidOnInitializer.isInitialized
    }

    override fun logRevenue(ad: Ad) {
        adaptersSource.adapters.filterIsInstance<AdRevenueLogger>()
            .forEach { adRevenueLogger ->
                adRevenueLogger.logAdRevenue(ad)
            }
    }
}
