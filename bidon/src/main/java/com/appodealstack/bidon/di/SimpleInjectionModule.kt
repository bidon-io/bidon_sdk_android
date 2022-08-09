package com.appodealstack.bidon.di

import com.appodealstack.bidon.Core
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.bidon.auctions.AdsRepository
import com.appodealstack.bidon.auctions.AdsRepositoryImpl
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.config.data.ConfigRequestInteractorImpl
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.core.AutoRefresher
import com.appodealstack.bidon.core.AutoRefresherImpl
import com.appodealstack.bidon.core.DemandsSource
import com.appodealstack.bidon.core.ListenersHolder
import com.appodealstack.bidon.core.impl.CoreImpl
import com.appodealstack.bidon.core.impl.DemandsSourceImpl
import com.appodealstack.bidon.core.impl.ListenersHolderImpl
import java.util.concurrent.atomic.AtomicBoolean

internal object SimpleInjectionModule {
    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializing Dependency Injection module
     */
    fun initDependencyInjection() {
        if (!isInitialized.getAndSet(true)) {
            registerDependencyInjection {
                factory<ConfigRequestInteractor> { ConfigRequestInteractorImpl() }
                factory<DemandsSource> { DemandsSourceImpl() }
                factory<ListenersHolder> { ListenersHolderImpl() }
                factory<AdRevenueInterceptorHolder> { AdRevenueInterceptorHolderImpl() }
                factory<AuctionResolversHolder> { AuctionResolversHolderImpl() }
                factory<AutoRefresher> {
                    AutoRefresherImpl(
                        adsRepository = get()
                    )
                }
                single<AdsRepository> { AdsRepositoryImpl() }
                single<Core> { CoreImpl() }
            }
        }
    }
}