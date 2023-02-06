package com.appodealstack.bidon.di

import android.app.Application
import android.content.Context
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.data.binderdatasources.DataProvider
import com.appodealstack.bidon.data.binderdatasources.DataProviderImpl
import com.appodealstack.bidon.data.binderdatasources.app.AppDataSource
import com.appodealstack.bidon.data.binderdatasources.app.AppDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.device.DeviceDataSource
import com.appodealstack.bidon.data.binderdatasources.device.DeviceDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.location.LocationDataSource
import com.appodealstack.bidon.data.binderdatasources.location.LocationDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.placement.PlacementDataSource
import com.appodealstack.bidon.data.binderdatasources.placement.PlacementDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSource
import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.session.SessionDataSource
import com.appodealstack.bidon.data.binderdatasources.session.SessionDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.session.SessionTracker
import com.appodealstack.bidon.data.binderdatasources.session.SessionTrackerImpl
import com.appodealstack.bidon.data.binderdatasources.token.TokenDataSource
import com.appodealstack.bidon.data.binderdatasources.token.TokenDataSourceImpl
import com.appodealstack.bidon.data.binderdatasources.user.AdvertisingData
import com.appodealstack.bidon.data.binderdatasources.user.UserDataSource
import com.appodealstack.bidon.data.binderdatasources.user.impl.AdvertisingDataImpl
import com.appodealstack.bidon.data.binderdatasources.user.impl.UserDataSourceImpl
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.data.networking.BidOnEndpoints
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.data.networking.NetworkStateObserver
import com.appodealstack.bidon.data.networking.impl.BidOnEndpointsImpl
import com.appodealstack.bidon.data.networking.impl.NetworkStateObserverImpl
import com.appodealstack.bidon.data.networking.requests.*
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import com.appodealstack.bidon.domain.adapter.impl.AdaptersSourceImpl
import com.appodealstack.bidon.domain.auction.Auction
import com.appodealstack.bidon.domain.auction.AuctionHolder
import com.appodealstack.bidon.domain.auction.RoundsListener
import com.appodealstack.bidon.domain.auction.impl.AuctionHolderImpl
import com.appodealstack.bidon.domain.auction.impl.AuctionImpl
import com.appodealstack.bidon.domain.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.impl.BidOnSdkImpl
import com.appodealstack.bidon.domain.common.usecases.CountDownTimer
import com.appodealstack.bidon.domain.config.AdapterInstanceCreator
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.impl.AdapterInstanceCreatorImpl
import com.appodealstack.bidon.domain.config.impl.BidOnInitializerImpl
import com.appodealstack.bidon.domain.config.impl.InitAndRegisterAdaptersUseCaseImpl
import com.appodealstack.bidon.domain.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.domain.config.usecases.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.domain.databinders.*
import com.appodealstack.bidon.domain.stats.usecases.SendImpressionRequestUseCase
import com.appodealstack.bidon.domain.stats.usecases.StatsRequestUseCase
import com.appodealstack.bidon.view.helper.GetOrientationUseCase
import com.appodealstack.bidon.view.helper.PauseResumeObserver
import com.appodealstack.bidon.view.helper.impl.ActivityLifecycleObserver
import com.appodealstack.bidon.view.helper.impl.GetOrientationUseCaseImpl
import com.appodealstack.bidon.view.helper.impl.PauseResumeObserverImpl

/**
 * Dependency Injection
 */
object DI {
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
            singleton<BidOnSdk> { BidOnSdkImpl() }

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
