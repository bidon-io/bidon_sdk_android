package com.appodeal.mads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appodeal.mads.databinding.ActivityMainBinding
import com.appodealstack.applovin.AppLovinSdkWrapper
import com.appodealstack.applovin.interstitial.MaxInterstitialAdWrapper
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val maxInterstitialAd: MaxInterstitialAd by lazy {
        MaxInterstitialAd("c7c5f664e60b9bfb", this)
    }

    private val maxInterstitialAdWrapper: MaxInterstitialAdWrapper by lazy {
        MaxInterstitialAdWrapper("c7c5f664e60b9bfb", this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        initViewsForApplovin(binding)
//        wannaApplovinInterstitial()

        initApplovin()
        initViewsForBidon(binding)
        wannaBidonInterstitial()
    }

    private fun initViewsForBidon(binding: ActivityMainBinding) {
        with(binding) {
            loadButton.setOnClickListener {
                println("Interstitial: loadAd clicked")
                maxInterstitialAdWrapper.loadAd()
            }
            showButton.setOnClickListener {
                if (maxInterstitialAdWrapper.isReady) {
                    println("Interstitial: showAd clicked")
                    maxInterstitialAdWrapper.showAd()
                } else {
                    println("Interstitial: showAd impossible. $maxInterstitialAdWrapper not ready.")
                }
            }
        }
    }

    private fun wannaBidonInterstitial() {
        maxInterstitialAdWrapper.setListener(object : AdListener {
            override fun onDemandAdLoaded(ad: AuctionData.Success) {
                super.onDemandAdLoaded(ad)
                println("MainActivity Interstitial: onDemandAdLoaded($ad)")
            }

            override fun onDemandAdLoadFailed(ad: AuctionData.Failure) {
                super.onDemandAdLoadFailed(ad)
                println("MainActivity Interstitial: onDemandAdLoadFailed(${ad.cause})")
            }

            override fun onWinnerFound(ads: List<AuctionData.Success>) {
                super.onWinnerFound(ads)
                println("MainActivity Interstitial: onWinnerFound($ads)")
            }

            override fun onAdLoaded(ad: AuctionData.Success) {
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                println("MainActivity Interstitial: onAdLoaded($ad)")
                ad.objResponse
            }

            override fun onAdDisplayed(ad: AuctionData.Success) {
                println("MainActivity Interstitial: onAdDisplayed($ad)")
            }

            override fun onAdDisplayFailed(ad: AuctionData.Failure) {
                maxInterstitialAd.loadAd()
            }

            override fun onAdHidden(ad: AuctionData.Success) {
                // Interstitial ad is hidden. Pre-load the next ad
                println("MainActivity Interstitial: onAdHidden($ad)")
                maxInterstitialAd.loadAd()
            }

            override fun onAdClicked(ad: AuctionData.Success) {
                println("MainActivity Interstitial: onAdClicked($ad)")
            }

            override fun onAdLoadFailed(cause: Throwable) {
                // Interstitial ad failed to load
                println("MainActivity Interstitial: onAdLoadFailed($cause)")
            }
        })
    }

    private fun initApplovin() {
        AppLovinSdkWrapper.getInstance(this).mediationProvider = "max"
        AppLovinSdkWrapper.initializeSdk(this) { appLovinSdkConfiguration ->
            println(appLovinSdkConfiguration)
        }
    }
}