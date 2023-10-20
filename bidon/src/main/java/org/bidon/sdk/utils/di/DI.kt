package org.bidon.sdk.utils.di

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.impl.AdaptersSourceImpl
import org.bidon.sdk.ads.banner.helper.CountDownTimer
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.ads.banner.helper.GetOrientationUseCase
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.ads.banner.helper.impl.ActivityLifecycleObserver
import org.bidon.sdk.ads.banner.helper.impl.GetOrientationUseCaseImpl
import org.bidon.sdk.ads.banner.helper.impl.PauseResumeObserverImpl
import org.bidon.sdk.ads.banner.render.AdRenderer
import org.bidon.sdk.ads.banner.render.AdRendererImpl
import org.bidon.sdk.ads.banner.render.CalculateAdContainerParamsUseCase
import org.bidon.sdk.ads.banner.render.RenderInspectorImpl
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.impl.AdCacheImpl
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.impl.AuctionImpl
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.impl.ResultsCollectorImpl
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.BidRequestUseCase
import org.bidon.sdk.auction.usecases.ConductBiddingRoundUseCase
import org.bidon.sdk.auction.usecases.ConductNetworkRoundUseCase
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.impl.AuctionStatImpl
import org.bidon.sdk.auction.usecases.impl.BidRequestUseCaseImpl
import org.bidon.sdk.auction.usecases.impl.ConductBiddingRoundUseCaseImpl
import org.bidon.sdk.auction.usecases.impl.ConductNetworkRoundUseCaseImpl
import org.bidon.sdk.auction.usecases.impl.ExecuteRoundUseCaseImpl
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
import org.bidon.sdk.databinders.location.LocationDataSource
import org.bidon.sdk.databinders.location.LocationDataSourceImpl
import org.bidon.sdk.databinders.placement.PlacementBinder
import org.bidon.sdk.databinders.placement.PlacementDataSource
import org.bidon.sdk.databinders.placement.PlacementDataSourceImpl
import org.bidon.sdk.databinders.reg.RegulationDataSource
import org.bidon.sdk.databinders.reg.RegulationDataSourceImpl
import org.bidon.sdk.databinders.reg.RegulationsBinder
import org.bidon.sdk.databinders.segment.SegmentBinder
import org.bidon.sdk.databinders.session.SessionBinder
import org.bidon.sdk.databinders.session.SessionDataSource
import org.bidon.sdk.databinders.session.SessionDataSourceImpl
import org.bidon.sdk.databinders.session.SessionTracker
import org.bidon.sdk.databinders.session.SessionTrackerImpl
import org.bidon.sdk.databinders.test.TestModeBinder
import org.bidon.sdk.databinders.token.TokenBinder
import org.bidon.sdk.databinders.token.TokenDataSource
import org.bidon.sdk.databinders.token.TokenDataSourceImpl
import org.bidon.sdk.databinders.user.AdvertisingData
import org.bidon.sdk.databinders.user.UserBinder
import org.bidon.sdk.databinders.user.UserDataSource
import org.bidon.sdk.databinders.user.impl.AdvertisingDataImpl
import org.bidon.sdk.databinders.user.impl.UserDataSourceImpl
import org.bidon.sdk.regulation.IabConsent
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.regulation.impl.IabConsentImpl
import org.bidon.sdk.regulation.impl.RegulationImpl
import org.bidon.sdk.segment.Segment
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.segment.impl.SegmentImpl
import org.bidon.sdk.stats.impl.SendImpressionRequestUseCaseImpl
import org.bidon.sdk.stats.impl.SendWinLossRequestUseCaseImpl
import org.bidon.sdk.stats.impl.StatsRequestUseCaseImpl
import org.bidon.sdk.stats.usecases.SendImpressionRequestUseCase
import org.bidon.sdk.stats.usecases.SendWinLossRequestUseCase
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorageImpl
import org.bidon.sdk.utils.networking.BidonEndpoints
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.NetworkStateObserver
import org.bidon.sdk.utils.networking.impl.BidonEndpointsImpl
import org.bidon.sdk.utils.networking.impl.NetworkStateObserverImpl
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCaseImpl
import org.bidon.sdk.utils.visibilitytracker.VisibilityTracker

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
        DeviceInfo.init(context)
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
            singleton<TokenDataSource> { TokenDataSourceImpl(keyValueStorage = get()) }
            singleton<Regulation> {
                RegulationImpl(
                    iabConsent = get()
                )
            }
            /**
             * [SegmentSynchronizer] depends on it
             */
            singleton<Segment> { SegmentImpl() }
            factory { get<Segment>() as SegmentSynchronizer }

            /**
             * Factories
             */
            factory<InitAndRegisterAdaptersUseCase> {
                InitAndRegisterAdaptersUseCaseImpl(
                    adaptersSource = get()
                )
            }
            factory<AdapterInstanceCreator> { AdapterInstanceCreatorImpl() }
            factory<AuctionResolver> { MaxEcpmAuctionResolver }
            factory<Auction> {
                AuctionImpl(
                    adaptersSource = get(),
                    getAuctionRequest = get(),
                    executeRound = get(),
                    auctionStat = get(),
                )
            }
            factory<AuctionStat> {
                AuctionStatImpl(
                    statsRequest = get(),
                    resolver = get()
                )
            }
            factoryWithParams { (param) ->
                CountDownTimer(
                    activityLifecycleObserver = param as ActivityLifecycleObserver
                )
            }
            factory<GetOrientationUseCase> { GetOrientationUseCaseImpl(context = get()) }
            factory { JsonHttpRequest(tokenDataSource = get()) }
            factory<ConductBiddingRoundUseCase> {
                ConductBiddingRoundUseCaseImpl(
                    bidRequestUseCase = get()
                )
            }
            factory<ConductNetworkRoundUseCase> {
                ConductNetworkRoundUseCaseImpl()
            }
            factory<BidRequestUseCase> {
                BidRequestUseCaseImpl(
                    createRequestBody = get(),
                    getOrientation = get(),
                )
            }
            factory<ExecuteRoundUseCase> {
                ExecuteRoundUseCaseImpl(
                    conductNetworkAuction = get(),
                    conductBiddingAuction = get(),
                    adaptersSource = get(),
                    regulation = get()
                )
            }

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
                    deviceBinder = DeviceBinder(deviceDataSource = get(), locationDataSource = get()),
                    appBinder = AppBinder(dataSource = get()),
                    sessionBinder = SessionBinder(dataSource = get()),
                    tokenBinder = TokenBinder(dataSource = get()),
                    userBinder = UserBinder(dataSource = get()),
                    placementBinder = PlacementBinder(dataSource = get()),
                    adaptersBinder = AdaptersBinder(adaptersSource = get()),
                    regulationsBinder = RegulationsBinder(dataSource = get()),
                    testModeBinder = TestModeBinder(),
                    segmentBinder = SegmentBinder(segmentSynchronizer = get()),
                )
            }
            factory<IabConsent> { IabConsentImpl(context = get()) }
            factory { VisibilityTracker() }
            factory<RegulationDataSource> {
                RegulationDataSourceImpl(
                    publisherRegulations = get(),
                    iabConsent = get()
                )
            }

            factory<SendWinLossRequestUseCase> {
                SendWinLossRequestUseCaseImpl(
                    createRequestBody = get()
                )
            }
            factory<ResultsCollector> {
                ResultsCollectorImpl(resolver = get())
            }
            factory<AdRenderer> {
                AdRendererImpl(
                    inspector = get(),
                    calculateAdContainerParams = get()
                )
            }
            factory<AdRenderer.RenderInspector> {
                RenderInspectorImpl()
            }
            factory { CalculateAdContainerParamsUseCase() }
            factoryWithParams<AdCache> { (demandAd) ->
                AdCacheImpl(
                    demandAd = demandAd as DemandAd,
                    scope = CoroutineScope(SdkDispatchers.Main),
                    resolver = get()
                )
            }
        }
    }
}
