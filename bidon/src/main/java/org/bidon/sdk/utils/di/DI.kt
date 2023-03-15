package org.bidon.sdk.utils.di

import android.app.Application
import android.content.Context
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.impl.AdaptersSourceImpl
import org.bidon.sdk.ads.banner.helper.CountDownTimer
import org.bidon.sdk.ads.banner.helper.GetOrientationUseCase
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.ads.banner.helper.impl.ActivityLifecycleObserver
import org.bidon.sdk.ads.banner.helper.impl.GetOrientationUseCaseImpl
import org.bidon.sdk.ads.banner.helper.impl.PauseResumeObserverImpl
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionHolder
import org.bidon.sdk.auction.RoundsListener
import org.bidon.sdk.auction.impl.AuctionHolderImpl
import org.bidon.sdk.auction.impl.AuctionImpl
import org.bidon.sdk.config.AdapterInstanceCreator
import org.bidon.sdk.config.impl.AdapterInstanceCreatorImpl
import org.bidon.sdk.config.impl.InitAndRegisterAdaptersUseCaseImpl
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.databinders.DataProvider
import org.bidon.sdk.databinders.DataProviderImpl
import org.bidon.sdk.databinders.adapters.AdaptersBinder
import org.bidon.sdk.databinders.app.AppBinder
import org.bidon.sdk.databinders.app.AppDataSource
import org.bidon.sdk.databinders.app.AppDataSourceImpl
import org.bidon.sdk.databinders.device.DeviceBinder
import org.bidon.sdk.databinders.device.DeviceDataSource
import org.bidon.sdk.databinders.device.DeviceDataSourceImpl
import org.bidon.sdk.databinders.geo.GeoBinder
import org.bidon.sdk.databinders.location.LocationDataSource
import org.bidon.sdk.databinders.location.LocationDataSourceImpl
import org.bidon.sdk.databinders.placement.PlacementBinder
import org.bidon.sdk.databinders.placement.PlacementDataSource
import org.bidon.sdk.databinders.placement.PlacementDataSourceImpl
import org.bidon.sdk.databinders.segment.SegmentBinder
import org.bidon.sdk.databinders.segment.SegmentDataSource
import org.bidon.sdk.databinders.segment.SegmentDataSourceImpl
import org.bidon.sdk.databinders.session.*
import org.bidon.sdk.databinders.token.TokenBinder
import org.bidon.sdk.databinders.token.TokenDataSource
import org.bidon.sdk.databinders.token.TokenDataSourceImpl
import org.bidon.sdk.databinders.user.AdvertisingData
import org.bidon.sdk.databinders.user.UserBinder
import org.bidon.sdk.databinders.user.UserDataSource
import org.bidon.sdk.databinders.user.impl.AdvertisingDataImpl
import org.bidon.sdk.databinders.user.impl.UserDataSourceImpl
import org.bidon.sdk.stats.impl.SendImpressionRequestUseCaseImpl
import org.bidon.sdk.stats.impl.StatsRequestUseCaseImpl
import org.bidon.sdk.stats.usecases.SendImpressionRequestUseCase
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorageImpl
import org.bidon.sdk.utils.networking.BidonEndpoints
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.NetworkStateObserver
import org.bidon.sdk.utils.networking.impl.BidonEndpointsImpl
import org.bidon.sdk.utils.networking.impl.NetworkStateObserverImpl
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCaseImpl

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
        FlavoredDI.init()
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
            singleton<BidonEndpoints> { BidonEndpointsImpl() }
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
            singleton<TokenDataSource> { TokenDataSourceImpl(keyValueStorage = get()) }

            /**
             * Factories
             */
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
            factory { JsonHttpRequest(tokenDataSource = get()) }

            /**
             * Requests
             */
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
