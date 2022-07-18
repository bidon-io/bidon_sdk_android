package com.appodeal.mads.ui

import android.app.Activity
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.admob.AdmobParameters
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.applovin.ApplovinMaxAdapter
import com.appodealstack.applovin.ApplovinParameters
import com.appodealstack.bidmachine.BidMachineAdapter
import com.appodealstack.bidmachine.BidMachineParameters
import com.appodealstack.fyber.FairBidAdapter
import com.appodealstack.fyber.FairBidDecorator
import com.appodealstack.fyber.FairBidParameters
import com.appodealstack.ironsource.IronSourceAdapter
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.IronSourceParameters
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import kotlinx.coroutines.flow.MutableStateFlow

internal class MainViewModel {
    data class State(
        val applovin: Boolean,
        val fyber: Boolean,
        val ironSource: Boolean,
    )

    val stateFlow = MutableStateFlow(
        State(
            applovin = false,
            fyber = false,
            ironSource = false
        )
    )

    fun setChecked(
        applovin: Boolean? = null,
        fyber: Boolean? = null,
        ironSource: Boolean? = null
    ) {
        val state = stateFlow.value
        stateFlow.value = State(
            applovin = applovin ?: state.applovin,
            fyber = fyber ?: state.fyber,
            ironSource = ironSource ?: state.ironSource,
        )
    }

    fun initSdk(
        activity: Activity,
        sdkApi: MediationSdk,
        onInitialized: () -> Unit
    ) {
        val state = stateFlow.value
        when (sdkApi) {
            MediationSdk.None -> error("Unexpected")
            MediationSdk.Applovin -> {
                sdkPairs.getValue(Demands.Admob).let { demand ->
                    AppLovinDecorator.register(demand.first, demand.second)
                }
                sdkPairs.getValue(Demands.BidMachine).let { demand ->
                    AppLovinDecorator.register(demand.first, demand.second)
                }
                if (state.fyber) {
                    sdkPairs.getValue(Demands.Fyber).let { demand ->
                        AppLovinDecorator.register(demand.first, demand.second)
                    }
                }
                if (state.ironSource) {
                    sdkPairs.getValue(Demands.IronSource).let { demand ->
                        AppLovinDecorator.register(demand.first, demand.second)
                    }
                }
                AppLovinDecorator.getInstance(activity).mediationProvider = "max"
                AppLovinDecorator
                    .initializeSdk(activity) {
                        onInitialized()
                    }
            }
            MediationSdk.Fyber -> {
                sdkPairs.getValue(Demands.Admob).let { demand ->
                    FairBidDecorator.register(demand.first, demand.second)
                }
                sdkPairs.getValue(Demands.BidMachine).let { demand ->
                    FairBidDecorator.register(demand.first, demand.second)
                }
                if (state.applovin) {
                    sdkPairs.getValue(Demands.Applovin).let { demand ->
                        FairBidDecorator.register(demand.first, demand.second)
                    }
                }
                if (state.ironSource) {
                    sdkPairs.getValue(Demands.IronSource).let { demand ->
                        FairBidDecorator.register(demand.first, demand.second)
                    }
                }
                FairBidDecorator
                    .start(
                        appId = "109613",
                        activity = activity,
                        onInitialized = {
                            onInitialized.invoke()
                        }
                    )
            }
            MediationSdk.IronSource -> {
                sdkPairs.getValue(Demands.Admob).let { demand ->
                    IronSourceDecorator.register(demand.first, demand.second)
                }
                sdkPairs.getValue(Demands.BidMachine).let { demand ->
                    IronSourceDecorator.register(demand.first, demand.second)
                }
                if (state.applovin) {
                    sdkPairs.getValue(Demands.Applovin).let { demand ->
                        IronSourceDecorator.register(demand.first, demand.second)
                    }
                }
                if (state.fyber) {
                    sdkPairs.getValue(Demands.Fyber).let { demand ->
                        IronSourceDecorator.register(demand.first, demand.second)
                    }
                }
                IronSourceDecorator
                    .init(
                        activity = activity,
                        appKey = "8545d445",
                        listener = {
                            onInitialized.invoke()
                        },
                    )
            }
        }
    }

    private enum class Demands {
        Admob,
        Applovin,
        BidMachine,
        Fyber,
        IronSource
    }

    private val sdkPairs =
        mapOf<Demands, Pair<Class<out Adapter<*>>, AdapterParameters>>(
            Demands.Admob to Pair(
                AdmobAdapter::class.java,
                AdmobParameters(
                    interstitials = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/1033173712",
                        1.0 to "ca-app-pub-3940256099942544/1033173712",
                        2.0 to "ca-app-pub-3940256099942544/1033173712",
                    ),
                    rewarded = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/5224354917",
                        1.0 to "ca-app-pub-3940256099942544/5224354917",
                        2.0 to "ca-app-pub-3940256099942544/5224354917",
                    ),
                    banners = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/6300978111",
                        1.0 to "ca-app-pub-3940256099942544/6300978111",
                        2.0 to "ca-app-pub-3940256099942544/6300978111",
                    ),
                )
            ),
            Demands.Applovin to Pair(
                ApplovinMaxAdapter::class.java,
                ApplovinParameters(
                    bannerAdUnitIds = listOf("c7c5f664e60b9bfb"),
                    interstitialAdUnitIds = listOf("c7c5f664e60b9bfb"),
                    rewardedAdUnitIds = listOf("c7c5f664e60b9bfb")
                )
            ),
            Demands.BidMachine to Pair(
                BidMachineAdapter::class.java,
                BidMachineParameters(sourceId = "1")
            ),
            Demands.Fyber to Pair(
                FairBidAdapter::class.java,
                FairBidParameters(
                    appKey = "109613",
                    interstitialPlacementIds = listOf("197405"),
                    rewardedPlacementIds = listOf("197406"),
                    bannerPlacementIds = listOf("197407")
                )
            ),
            Demands.IronSource to Pair(
                IronSourceAdapter::class.java,
                IronSourceParameters(appKey = "8545d445")
            ),
        )
}