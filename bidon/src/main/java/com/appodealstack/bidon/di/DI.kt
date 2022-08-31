package com.appodealstack.bidon.di

import android.app.Application
import android.content.Context
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
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.impl.AdaptersSourceImpl
import com.appodealstack.bidon.core.impl.BidOnSdkImpl
import com.appodealstack.bidon.core.impl.PauseResumeObserverImpl
import com.appodealstack.bidon.utilities.datasource.app.AppDataSource
import com.appodealstack.bidon.utilities.datasource.app.AppDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.device.DeviceDataSource
import com.appodealstack.bidon.utilities.datasource.device.DeviceDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.location.LocationDataSource
import com.appodealstack.bidon.utilities.datasource.location.LocationDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.placement.PlacementDataSource
import com.appodealstack.bidon.utilities.datasource.placement.PlacementDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.session.SessionDataSource
import com.appodealstack.bidon.utilities.datasource.session.SessionDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.session.SessionTracker
import com.appodealstack.bidon.utilities.datasource.session.SessionTrackerImpl
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSource
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfo
import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfoImpl
import com.appodealstack.bidon.utilities.datasource.user.UserDataSource
import com.appodealstack.bidon.utilities.datasource.user.UserDataSourceImpl
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import com.appodealstack.bidon.utilities.network.endpoint.BidOnEndpointsImpl
import com.appodealstack.bidon.view.BannerAd
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dependency Injection
 */
object DI {
    private val isInitialized = AtomicBoolean(false)

    fun init(context: Context) {
        registerDependencyInjection {
            singleton<Context> { context.applicationContext }
        }
    }

    /**
     * Initializing Dependency Injection module
     */
    fun initDependencyInjection() {
        if (!isInitialized.getAndSet(true)) {
            registerDependencyInjection {
                /**
                 * Singletons
                 */
                singleton<BidOnSdk> { BidOnSdkImpl() }

                singleton<AdaptersSource> { AdaptersSourceImpl() }
                singleton<BidOnEndpoints> { BidOnEndpointsImpl() }
                singleton<KeyValueStorage> {
                    KeyValueStorageImpl(
                        context = get()
                    )
                }
                singleton<PauseResumeObserver> {
                    @Suppress("UNCHECKED_CAST")
                    PauseResumeObserverImpl(
                        application = get<Context>() as Application
                    )
                }
                singleton<AdvertisingInfo> { AdvertisingInfoImpl() }
                singleton<LocationDataSource> { LocationDataSourceImpl(context = get()) }
                singleton<SessionDataSource> {
                    SessionDataSourceImpl(
                        context = get(),
                        sessionTracker = get()
                    )
                }
                singleton<SessionTracker> {
                    SessionTrackerImpl(
                        context = get(),
                        pauseResumeObserver = get()
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
                        keyValueStorage = get(),
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
                factory { DeviceBinder(dataSource = get()) }
                factory { AppBinder(dataSource = get()) }
                factory { GeoBinder(dataSource = get()) }
                factory { SessionBinder(dataSource = get()) }
                factory { TokenBinder(dataSource = get()) }
                factory { UserBinder(dataSource = get()) }
                factory { PlacementBinder(dataSource = get()) }

                factory<AppDataSource> { AppDataSourceImpl(context = get(), keyValueStorage = get()) }
                factory<DeviceDataSource> { DeviceDataSourceImpl(context = get()) }
                factory<TokenDataSource> { TokenDataSourceImpl(keyValueStorage = get()) }
                factory<UserDataSource> { UserDataSourceImpl(consentFactory = { null }) } // TODO Add ConsentManager
                factory<PlacementDataSource> { PlacementDataSourceImpl() }

                factory<DataProvider> {
                    DataProviderImpl(
                        deviceBinder = get(),
                        appBinder = get(),
                        geoBinder = get(),
                        sessionBinder = get(),
                        tokenBinder = get(),
                        userBinder = get(),
                        placementBinder = get()
                    )
                }
            }
        }
    }
}
