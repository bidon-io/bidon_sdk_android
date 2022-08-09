package com.appodealstack.bidon

import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.bidon.auctions.AdsRepository
import com.appodealstack.bidon.auctions.AdsRepositoryImpl
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.auctions.impl.AuctionResolversHolderImpl
import com.appodealstack.bidon.config.data.ConfigRequestInteractorImpl
import com.appodealstack.bidon.config.domain.AdapterInfo
import com.appodealstack.bidon.config.domain.ConfigRequestBody
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.core.AutoRefresher
import com.appodealstack.bidon.core.AutoRefresherImpl
import com.appodealstack.bidon.core.DemandsSource
import com.appodealstack.bidon.core.ListenersHolder
import com.appodealstack.bidon.core.impl.DemandsSourceImpl
import com.appodealstack.bidon.core.impl.ListenersHolderImpl
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.di.inject
import com.appodealstack.bidon.di.registerDependencyInjection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TempApi {
    private const val BaseUrl = "https://herokuapp.appodeal.com/android_bidon_config"
//    private const val BaseUrl = "https://run.mocky.io/v3/a53f8ae1-f0c5-4e57-b25b-78f3831fb947"
//    private const val BaseUrl = "https://1e69e7f9-a8f2-4cc2-9d30-5a71dd5d6db2.mock.pstmn.io"

    fun start() {
        registerDependencyInjection {
            factory<ConfigRequestInteractor> {
                ConfigRequestInteractorImpl()
            }
            factory<DemandsSource> {
                DemandsSourceImpl()
            }
            factory<ListenersHolder> {
                ListenersHolderImpl()
            }
            factory<AdRevenueInterceptorHolder> {
                AdRevenueInterceptorHolderImpl()
            }
            factory<AuctionResolversHolder> {
                AuctionResolversHolderImpl()
            }
            factory<AutoRefresher> {
                AutoRefresherImpl(
                    adsRepository = get()
                )
            }
            single<AdsRepository> {
                AdsRepositoryImpl()
            }
        }
        val interactor by inject<ConfigRequestInteractor>()

        GlobalScope.launch {
            interactor.request(
                body = ConfigRequestBody(
                    adapters = listOf(
                        AdapterInfo("123", "4321")
                    )
                )
            ).onSuccess {

            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}