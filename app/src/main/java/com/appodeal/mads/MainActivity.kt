package com.appodeal.mads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appodeal.mads.databinding.ActivityMainBinding
import com.appodealstack.applovin.AppLovinSdkWrapper
import com.appodealstack.applovin.interstitial.MaxInterstitialAdWrapper
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.BidOnAd
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
            override fun onAdLoaded(ad: BidOnAd?) {
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                println("Interstitial: onAdLoaded($ad)")
            }

            override fun onAdDisplayed(ad: BidOnAd?) {
                println("Interstitial: onAdDisplayed($ad)")
            }

            override fun onAdHidden(ad: BidOnAd?) {
                // Interstitial ad is hidden. Pre-load the next ad
                println("Interstitial: onAdHidden($ad)")
                maxInterstitialAd.loadAd()
            }

            override fun onAdClicked(ad: BidOnAd?) {
                println("Interstitial: onAdClicked($ad)")
            }

            override fun onAdLoadFailed(adUnitId: String?, error: DemandError?) {
                // Interstitial ad failed to load

                if (error is MaxError) {
                    // AppLovin recommends that you retry with exponentially higher delays up to a maximum delay (in this case 64 seconds)
                    println("Interstitial: onAdLoadFailed($adUnitId). Error: ${error.message}")
                }

                lifecycleScope.launch {
                    delay(5000)
                    maxInterstitialAd.loadAd()
                }
            }

            override fun onAdDisplayFailed(ad: BidOnAd?, error: DemandError?) {
                maxInterstitialAd.loadAd()
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