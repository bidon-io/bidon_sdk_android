package com.appodealstack.bidon.di

import com.appodealstack.bidon.BidONSdk
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.bidon.auctions.AdsRepository
import com.appodealstack.bidon.auctions.AdsRepositoryImpl
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.config.data.impl.AdapterInstanceCreatorImpl
import com.appodealstack.bidon.config.data.impl.ConfigRequestInteractorImpl
import com.appodealstack.bidon.config.domain.*
import com.appodealstack.bidon.config.domain.databinders.*
import com.appodealstack.bidon.config.domain.impl.BidONInitializerImpl
import com.appodealstack.bidon.config.domain.impl.DataProviderImpl
import com.appodealstack.bidon.config.domain.impl.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.core.*
import com.appodealstack.bidon.core.impl.*
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
                /**
                 * Singletons
                 */
                singleton<BidONSdk> {
                    BidONSdkImpl(
                        bidONInitializer = get(),
                        contextProvider = get()
                    )
                }
                singleton<AdsRepository> { AdsRepositoryImpl() }
                singleton<Core> { CoreImpl() }
                singleton<ContextProvider> { ContextProviderImpl() }

                /**
                 * Factories
                 */
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

                factory<ConfigRequestInteractor> {
                    ConfigRequestInteractorImpl(
                        dataProvider = get()
                    )
                }
                factory<AdaptersSource> { AdaptersSourceImpl() }
                factory<ListenersHolder> { ListenersHolderImpl() }
                factory<AdRevenueInterceptorHolder> { AdRevenueInterceptorHolderImpl() }
                factory<AuctionResolversHolder> { AuctionResolversHolderImpl() }
                factory<AutoRefresher> {
                    AutoRefresherImpl(
                        adsRepository = get()
                    )
                }

                /**
                 * Binders
                 */
                factory<DataProvider> {
                    DataProviderImpl(
                        deviceBinder = get(),
                        appBinder = get(),
                        geoBinder = get(),
                        sessionBinder = get(),
                        tokenBinder = get(),
                        userBinder = get(),
                    )
                }
                factory { DeviceBinder(contextProvider = get()) }
                factory { AppBinder() }
                factory { GeoBinder() }
                factory { SessionBinder() }
                factory { TokenBinder() }
                factory { UserBinder() }
            }
        }
    }
}