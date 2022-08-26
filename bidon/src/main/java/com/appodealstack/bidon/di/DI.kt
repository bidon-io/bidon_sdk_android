package com.appodealstack.bidon.di

import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.data.impl.GetAuctionRequestUseCaseImpl
import com.appodealstack.bidon.auctions.domain.AdsRepository
import com.appodealstack.bidon.auctions.domain.GetAuctionRequestUseCase
import com.appodealstack.bidon.auctions.domain.NewAuction
import com.appodealstack.bidon.auctions.domain.impl.AdsRepositoryImpl
import com.appodealstack.bidon.auctions.domain.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.auctions.domain.impl.NewAuctionImpl
import com.appodealstack.bidon.config.data.impl.AdapterInstanceCreatorImpl
import com.appodealstack.bidon.config.data.impl.GetConfigRequestUseCaseImpl
import com.appodealstack.bidon.config.domain.*
import com.appodealstack.bidon.config.domain.databinders.*
import com.appodealstack.bidon.config.domain.impl.BidOnInitializerImpl
import com.appodealstack.bidon.config.domain.impl.DataProviderImpl
import com.appodealstack.bidon.config.domain.impl.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.core.*
import com.appodealstack.bidon.core.impl.*
import com.appodealstack.bidon.utilities.datasource.DataSourceProvider
import com.appodealstack.bidon.utilities.datasource.DataSourceProvider.getDataSource
import com.appodealstack.bidon.utilities.datasource.SourceType
import com.appodealstack.bidon.utilities.datasource.app.AppDataSource
import com.appodealstack.bidon.utilities.datasource.device.DeviceDataSource
import com.appodealstack.bidon.utilities.datasource.location.LocationDataSource
import com.appodealstack.bidon.utilities.datasource.placement.PlacementDataSource
import com.appodealstack.bidon.utilities.datasource.session.SessionDataSource
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSource
import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfo
import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfoImpl
import com.appodealstack.bidon.utilities.datasource.user.UserDataSource
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import com.appodealstack.bidon.utilities.network.endpoint.BidOnEndpointsImpl
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
                singleton<AdsRepository> { AdsRepositoryImpl() }
                singleton<Core> { CoreImpl() }
                singleton<ContextProvider> { ContextProviderImpl() }
                singleton<AdaptersSource> { AdaptersSourceImpl() }
                singleton<BidOnEndpoints> { BidOnEndpointsImpl() }
                singleton<KeyValueStorage> { KeyValueStorageImpl() }
                singleton<AdvertisingInfo> { AdvertisingInfoImpl() }

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
                factory<AdRevenueInterceptorHolder> { AdRevenueInterceptorHolderImpl() }
                factory<AuctionResolversHolder> { AuctionResolversHolderImpl() }
                factory<NewAuction> {
                    NewAuctionImpl(
                        adaptersSource = get(),
                        contextProvider = get(),
                        getAuctionRequest = get()
                    )
                }
                factory<AutoRefresher> {
                    AutoRefresherImpl(
                        adsRepository = get()
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
                        placementBinder = get()
                    )
                }
                factory { DeviceBinder(getDataSource(type = SourceType.Device, contextProvider = get()) as DeviceDataSource) }
                factory { AppBinder(getDataSource(type = SourceType.App, contextProvider = get()) as AppDataSource) }
                factory { GeoBinder(getDataSource(type = SourceType.Location, contextProvider = get()) as LocationDataSource) }
                factory { SessionBinder(getDataSource(type = SourceType.Session, contextProvider = get()) as SessionDataSource)  }
                factory { TokenBinder(getDataSource(type = SourceType.Token, contextProvider = get()) as TokenDataSource)  }
                factory { UserBinder(getDataSource(type = SourceType.User, contextProvider = get()) as UserDataSource) }
                factory { PlacementBinder(getDataSource(type = SourceType.Placement, contextProvider = get()) as PlacementDataSource) }
            }
        }
    }
}