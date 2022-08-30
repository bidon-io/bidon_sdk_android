package com.appodealstack.bidon.di

import android.app.Application
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.data.impl.GetAuctionRequestUseCaseImpl
import com.appodealstack.bidon.auctions.domain.*
import com.appodealstack.bidon.auctions.domain.impl.AuctionHolderImpl
import com.appodealstack.bidon.auctions.domain.impl.AuctionImpl
import com.appodealstack.bidon.auctions.domain.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.config.data.impl.AdapterInstanceCreatorImpl
import com.appodealstack.bidon.config.data.impl.GetConfigRequestUseCaseImpl
import com.appodealstack.bidon.config.domain.*
import com.appodealstack.bidon.config.domain.databinders.*
import com.appodealstack.bidon.config.domain.impl.BidOnInitializerImpl
import com.appodealstack.bidon.config.domain.impl.DataProviderImpl
import com.appodealstack.bidon.config.domain.impl.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.impl.AdaptersSourceImpl
import com.appodealstack.bidon.core.impl.BidOnSdkImpl
import com.appodealstack.bidon.core.impl.ContextProviderImpl
import com.appodealstack.bidon.core.impl.PauseResumeObserverImpl
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import com.appodealstack.bidon.utilities.network.endpoint.BidOnEndpointsImpl
import com.appodealstack.bidon.view.BannerAd
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
                singleton<BidOnSdk> {
                    BidOnSdkImpl(
                        bidONInitializer = get(),
                        contextProvider = get(),
                        adaptersSource = get(),
                        bidOnEndpoints = get()
                    )
                }
                singleton<ContextProvider> { ContextProviderImpl() }
                singleton<AdaptersSource> { AdaptersSourceImpl() }
                singleton<BidOnEndpoints> { BidOnEndpointsImpl() }
                singleton<KeyValueStorage> { KeyValueStorageImpl() }
                singleton<PauseResumeObserver> {
                    @Suppress("UNCHECKED_CAST")
                    PauseResumeObserverImpl(
                        application = get<ContextProvider>().requiredContext.applicationContext as Application
                    )
                }

                /**
                 * Factories
                 */
                factory<BidOnInitializer> {
                    BidOnInitializerImpl(
                        initAndRegisterAdapters = get(),
                        getConfigRequest = get(),
                        adapterInstanceCreator = get(),
                        keyValueStorage = get()
                    )
                }
                factory<InitAndRegisterAdaptersUseCase> {
                    InitAndRegisterAdaptersUseCaseImpl(
                        adaptersSource = get()
                    )
                }
                factory<AdapterInstanceCreator> { AdapterInstanceCreatorImpl() }
                factory<AuctionResolversHolder> { AuctionResolversHolderImpl() }
                factory<Auction> {
                    AuctionImpl(
                        adaptersSource = get(),
                        getAuctionRequest = get()
                    )
                }
                factoryWithParams<AutoRefresher> { param ->
                    AutoRefresherImpl(autoRefreshable = param as BannerAd.AutoRefreshable)
                }
                factory {
                    CountDownTimer(
                        pauseResumeObserver = get()
                    )
                }

                @Suppress("UNCHECKED_CAST")
                factoryWithParams<AuctionHolder> { param ->
                    val (demandAd, listener) = param as Pair<DemandAd, RoundsListener>
                    AuctionHolderImpl(
                        demandAd = demandAd,
                        roundsListener = listener
                    )
                }

                /**
                 * Requests
                 */
                factory<GetConfigRequestUseCase> {
                    GetConfigRequestUseCaseImpl(
                        dataProvider = get(),
                        keyValueStorage = get()
                    )
                }
                factory<GetAuctionRequestUseCase> {
                    GetAuctionRequestUseCaseImpl(
                        dataProvider = get(),
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
