package com.appodealstack.bidon.di

import com.appodealstack.bidon.BidONSdk
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.bidon.auctions.AdsRepository
import com.appodealstack.bidon.auctions.AdsRepositoryImpl
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.config.data.AdapterInstanceCreatorImpl
import com.appodealstack.bidon.config.data.BidONInitializerImpl
import com.appodealstack.bidon.config.data.ConfigRequestInteractorImpl
import com.appodealstack.bidon.config.data.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.config.domain.AdapterInstanceCreator
import com.appodealstack.bidon.config.domain.BidONInitializer
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.config.domain.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.AutoRefresher
import com.appodealstack.bidon.core.AutoRefresherImpl
import com.appodealstack.bidon.core.ListenersHolder
import com.appodealstack.bidon.core.impl.AdaptersSourceImpl
import com.appodealstack.bidon.core.impl.BidONSdkImpl
import com.appodealstack.bidon.core.impl.CoreImpl
import com.appodealstack.bidon.core.impl.ListenersHolderImpl
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dependency Injection
 */
internal object DI {
    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializing Dependency Injection module
     */
    fun initDependencyInjection() {
        if (!isInitialized.getAndSet(true)) {
            registerDependencyInjection {
                single<BidONSdk> {
                    BidONSdkImpl(
                        bidONInitializer = get()
                    )
                }

                factory<BidONInitializer> {
                    BidONInitializerImpl(
                        initAndRegisterAdapters = get(),
                        configRequestInteractor = get(),
                        adapterInstanceCreator = get()
                    )
                }
                factory<InitAndRegisterAdaptersUseCase> {
                    InitAndRegisterAdaptersUseCaseImpl(
                        adaptersSource = get<Core>() as AdaptersSource
                    )
                }
                factory<AdapterInstanceCreator> { AdapterInstanceCreatorImpl() }

                factory<ConfigRequestInteractor> { ConfigRequestInteractorImpl() }
                factory<AdaptersSource> { AdaptersSourceImpl() }
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