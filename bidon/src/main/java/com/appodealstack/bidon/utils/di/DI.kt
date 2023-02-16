package com.appodealstack.bidon.utils.di

import android.app.Application
import android.content.Context
import com.appodealstack.bidon.adapter.AdaptersSource
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.adapter.impl.AdaptersSourceImpl
import com.appodealstack.bidon.ads.banner.helper.CountDownTimer
import com.appodealstack.bidon.ads.banner.helper.GetOrientationUseCase
import com.appodealstack.bidon.ads.banner.helper.PauseResumeObserver
import com.appodealstack.bidon.ads.banner.helper.impl.ActivityLifecycleObserver
import com.appodealstack.bidon.ads.banner.helper.impl.GetOrientationUseCaseImpl
import com.appodealstack.bidon.ads.banner.helper.impl.PauseResumeObserverImpl
import com.appodealstack.bidon.auction.Auction
import com.appodealstack.bidon.auction.AuctionHolder
import com.appodealstack.bidon.auction.RoundsListener
import com.appodealstack.bidon.auction.impl.AuctionHolderImpl
import com.appodealstack.bidon.auction.impl.AuctionImpl
import com.appodealstack.bidon.auction.impl.GetAuctionRequestUseCaseImpl
import com.appodealstack.bidon.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.config.impl.GetConfigRequestUseCaseImpl
import com.appodealstack.bidon.config.impl.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.config.usecases.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.databinders.DataProvider
import com.appodealstack.bidon.databinders.DataProviderImpl
import com.appodealstack.bidon.databinders.adapters.AdaptersBinder
import com.appodealstack.bidon.databinders.app.AppBinder
import com.appodealstack.bidon.databinders.app.AppDataSource
import com.appodealstack.bidon.databinders.app.AppDataSourceImpl
import com.appodealstack.bidon.databinders.device.DeviceBinder
import com.appodealstack.bidon.databinders.device.DeviceDataSource
import com.appodealstack.bidon.databinders.device.DeviceDataSourceImpl
import com.appodealstack.bidon.databinders.geo.GeoBinder
import com.appodealstack.bidon.databinders.location.LocationDataSource
import com.appodealstack.bidon.databinders.location.LocationDataSourceImpl
import com.appodealstack.bidon.databinders.placement.PlacementBinder
import com.appodealstack.bidon.databinders.placement.PlacementDataSource
import com.appodealstack.bidon.databinders.placement.PlacementDataSourceImpl
import com.appodealstack.bidon.databinders.segment.SegmentBinder
import com.appodealstack.bidon.databinders.segment.SegmentDataSource
import com.appodealstack.bidon.databinders.segment.SegmentDataSourceImpl
import com.appodealstack.bidon.databinders.session.*
import com.appodealstack.bidon.databinders.token.TokenBinder
import com.appodealstack.bidon.databinders.token.TokenDataSource
import com.appodealstack.bidon.databinders.token.TokenDataSourceImpl
import com.appodealstack.bidon.databinders.user.AdvertisingData
import com.appodealstack.bidon.databinders.user.UserBinder
import com.appodealstack.bidon.databinders.user.UserDataSource
import com.appodealstack.bidon.databinders.user.impl.AdvertisingDataImpl
import com.appodealstack.bidon.databinders.user.impl.UserDataSourceImpl
import com.appodealstack.bidon.stats.impl.SendImpressionRequestUseCaseImpl
import com.appodealstack.bidon.stats.impl.StatsRequestUseCaseImpl
import com.appodealstack.bidon.stats.usecases.SendImpressionRequestUseCase
import com.appodealstack.bidon.stats.usecases.StatsRequestUseCase
import com.appodealstack.bidon.utils.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utils.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.utils.networking.BidOnEndpoints
import com.appodealstack.bidon.utils.networking.JsonHttpRequest
import com.appodealstack.bidon.utils.networking.NetworkStateObserver
import com.appodealstack.bidon.utils.networking.impl.BidOnEndpointsImpl
import com.appodealstack.bidon.utils.networking.impl.NetworkStateObserverImpl
import com.appodealstack.bidon.utils.networking.requests.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Dependency Injection
 */
internal object DI {
    fun init(context: Context) {
        module {
            singleton<Context> { context.applicationContext }
        }
    }

    /**
     * Initializing Dependency Injection module
     */
    fun setFactories() {
        module {
            /**
             * Singletons
             */
            singleton<AdaptersSource> { AdaptersSourceImpl() }
            singleton<BidOnEndpoints> { BidOnEndpointsImpl() }
            singleton<KeyValueStorage> {
                KeyValueStorageImpl(
                    context = get()
                )
            }
            singleton<PauseResumeObserver> {
                PauseResumeObserverImpl(
                    application = get<Context>() as Application
                )
            }
            singleton<AdvertisingData> {
                AdvertisingDataImpl(
                    context = get()
                )
            }
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
            singleton<NetworkStateObserver> { NetworkStateObserverImpl() }

            // [SegmentDataSource] should be singleton per session
            singleton<SegmentDataSource> { SegmentDataSourceImpl() }

            /**
             * Factories
             */
            factory<InitAndRegisterAdaptersUseCase> {
                InitAndRegisterAdaptersUseCaseImpl(
                    adaptersSource = get()
                )
            }
            factory<com.appodealstack.bidon.config.AdapterInstanceCreator> { com.appodealstack.bidon.config.impl.AdapterInstanceCreatorImpl() }
            factory<Auction> {
                AuctionImpl(
                    adaptersSource = get(),
                    getAuctionRequest = get(),
                    statsRequest = get(),
                )
            }
            factoryWithParams { (param) ->
                CountDownTimer(
                    activityLifecycleObserver = param as ActivityLifecycleObserver
                )
            }

            factoryWithParams<AuctionHolder> { (demandAd, listener) ->
                AuctionHolderImpl(
                    demandAd = demandAd as DemandAd,
                    roundsListener = listener as RoundsListener
                )
            }
            factory<GetOrientationUseCase> { GetOrientationUseCaseImpl(context = get()) }
            factory { JsonHttpRequest(keyValueStorage = get()) }

            /**
             * Requests
             */
            factory<GetConfigRequestUseCase> {
                GetConfigRequestUseCaseImpl(
                    createRequestBody = get(),
                    segmentDataSource = get()
                )
            }
            factory<GetAuctionRequestUseCase> {
                GetAuctionRequestUseCaseImpl(
                    createRequestBody = get(),
                    getOrientation = get()
                )
            }
            factory<StatsRequestUseCase> {
                StatsRequestUseCaseImpl(
                    createRequestBody = get(),
                )
            }
            factory<SendImpressionRequestUseCase> {
                SendImpressionRequestUseCaseImpl(
                    createRequestBody = get(),
                )
            }

            /**
             * Binders
             */
            factory<AppDataSource> { AppDataSourceImpl(context = get(), keyValueStorage = get()) }
            factory<DeviceDataSource> { DeviceDataSourceImpl(context = get()) }
            factory<TokenDataSource> { TokenDataSourceImpl(keyValueStorage = get()) }

            factory<UserDataSource> {
                UserDataSourceImpl(
                    keyValueStorage = get(),
                    advertisingData = get()
                )
            }
            factory<PlacementDataSource> { PlacementDataSourceImpl() }
            factory<CreateRequestBodyUseCase> {
                CreateRequestBodyUseCaseImpl(
                    dataProvider = get()
                )
            }

            factory<DataProvider> {
                DataProviderImpl(
                    deviceBinder = DeviceBinder(dataSource = get()),
                    appBinder = AppBinder(dataSource = get()),
                    geoBinder = GeoBinder(dataSource = get()),
                    sessionBinder = SessionBinder(dataSource = get()),
                    tokenBinder = TokenBinder(dataSource = get()),
                    userBinder = UserBinder(dataSource = get()),
                    placementBinder = PlacementBinder(dataSource = get()),
                    adaptersBinder = AdaptersBinder(adaptersSource = get()),
                    segmentBinder = SegmentBinder(dataSource = get()),
                )
            }
        }
    }
}
