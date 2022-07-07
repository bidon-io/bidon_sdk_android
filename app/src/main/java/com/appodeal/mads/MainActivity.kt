package com.appodeal.mads

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.appodeal.mads.databinding.ActivityMainBinding
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.applovin.interstitial.BNInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.applovin.rewarded.BNMaxRewardedAd
import com.appodealstack.applovin.rewarded.BNRewardedListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.RewardedAdListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val bannerAd: BNMaxAdView by lazy {
        BNMaxAdView("c7c5f664e60b9bfb", this)
    }

    private val interstitialAd: BNMaxInterstitialAd by lazy {
        BNMaxInterstitialAd("c7c5f664e60b9bfb", this)
    }

    private val rewardedAd: BNMaxRewardedAd by lazy {
        BNMaxRewardedAd("c7c5f664e60b9bfb", this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsForBidon(binding)
        setInterstitialListener()
        setRewardedListener()
        setBannerListener()
    }

    private fun setRewardedListener() {
        rewardedAd.setListener(object : BNRewardedListener {
            override fun onRewardedStarted(ad: Ad) {
                log(line = "onRewardedStarted: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onRewardedStarted($ad)")
            }

            override fun onRewardedCompleted(ad: Ad) {
                log(line = "onRewardedCompleted: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onRewardedCompleted($ad)")
            }

            override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
                log(line = "onUserRewarded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onUserRewarded($ad, $reward)")
            }

            override fun onAdLoaded(ad: Ad) {
                log(line = "onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onAdLoaded($ad)")
            }

            override fun onAdLoadFailed(cause: Throwable) {
                log(line = "onAdLoadFailed: $cause")
                println("MainActivity Rewarded: onAdLoadFailed($cause)")
            }

            override fun onAdDisplayed(ad: Ad) {
                log(line = "onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onAdDisplayed($ad)")
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                log(line = "onAdDisplayFailed: $cause")
                println("MainActivity Rewarded: onAdDisplayFailed($cause)")
            }

            override fun onAdClicked(ad: Ad) {
                log(line = "onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onAdClicked($ad)")
            }

            override fun onAdHidden(ad: Ad) {
                log(line = "onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onAdHidden($ad)")
            }

            override fun onDemandAdLoaded(ad: Ad) {
                super.onDemandAdLoaded(ad)
                log(line = "onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Rewarded: onDemandAdLoaded($ad)")
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                super.onDemandAdLoadFailed(cause)
                log(line = "onDemandAdLoadFailed: $cause")
                println("MainActivity Rewarded: onDemandAdLoadFailed($cause)")
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                super.onAuctionFinished(ads)
                val str = StringBuilder()
                str.appendLine("onWinnerFound")
                ads.forEachIndexed { i, ad ->
                    str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
                }
                log(line = str.toString())
                println("MainActivity Rewarded: onWinnerFound($ads)")
            }
        })
    }

    private fun initViewsForBidon(binding: ActivityMainBinding) {
        with(binding) {
            initButton.setOnClickListener {
                initApplovin()
            }
            loadButton.setOnClickListener {
                println("Rewarded: loadAd clicked")
                interstitialAd.loadAd()
            }
            showButton.setOnClickListener {
                if (interstitialAd.isReady) {
                    println("Interstitial: showAd clicked")
                    interstitialAd.showAd()
                } else {
                    println("Interstitial: showAd impossible. $interstitialAd not ready.")
                }
            }
            loadRewardedButton.setOnClickListener {
                println("Rewarded: loadAd clicked")
                rewardedAd.loadAd()
            }
            showRewardedButton.setOnClickListener {
                if (rewardedAd.isReady) {
                    println("Rewarded: showAd clicked")
                    rewardedAd.showAd()
                } else {
                    println("Rewarded: showAd impossible. $rewardedAd not ready.")
                }
            }
            loadBannerButton.setOnClickListener {
                bannerAd.loadAd()
            }
            adBannerView.addView(bannerAd)
        }
    }

    private fun setInterstitialListener() {
        interstitialAd.setListener(object : BNInterstitialListener {
            override fun onDemandAdLoaded(ad: Ad) {
                super.onDemandAdLoaded(ad)
                log(line = "onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Interstitial: onDemandAdLoaded($ad)")
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                super.onDemandAdLoadFailed(cause)
                log(line = "onDemandAdLoadFailed: $cause")
                println("MainActivity Interstitial: onDemandAdLoadFailed($cause)")
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                super.onAuctionFinished(ads)
                val str = StringBuilder()
                str.appendLine("onWinnerFound")
                ads.forEachIndexed { i, ad ->
                    str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
                }
                log(line = str.toString())
                println("MainActivity Interstitial: onWinnerFound($ads)")
            }

            override fun onAdLoaded(ad: Ad) {
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                log(line = "onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Interstitial: onAdLoaded($ad)")
            }

            override fun onAdDisplayed(ad: Ad) {
                log(line = "onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Interstitial: onAdDisplayed($ad)")
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                log(line = "onAdDisplayFailed: $cause")
                println("MainActivity Interstitial: onAdDisplayed($cause)")
            }

            override fun onAdHidden(ad: Ad) {
                log(line = "onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
                // Interstitial ad is hidden. Pre-load the next ad
                println("MainActivity Interstitial: onAdHidden($ad)")
            }

            override fun onAdClicked(ad: Ad) {
                log(line = "onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Interstitial: onAdClicked($ad)")
            }

            override fun onAdLoadFailed(cause: Throwable) {
                // Interstitial ad failed to load
                println("MainActivity Interstitial: onAdLoadFailed($cause)")
            }
        })
    }

    private fun setBannerListener() {
        bannerAd.setListener(object : BNMaxAdViewAdListener {
            override fun onDemandAdLoaded(ad: Ad) {
                super.onDemandAdLoaded(ad)
                log(line = "onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Banner: onDemandAdLoaded($ad)")
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                super.onDemandAdLoadFailed(cause)
                log(line = "onDemandAdLoadFailed: $cause")
                println("MainActivity Banner: onDemandAdLoadFailed($cause)")
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                super.onAuctionFinished(ads)
                val str = StringBuilder()
                str.appendLine("onWinnerFound")
                ads.forEachIndexed { i, ad ->
                    str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
                }
                log(line = str.toString())
                println("MainActivity Banner: onWinnerFound($ads)")
            }

            override fun onAdExpanded(ad: Ad) {
            }

            override fun onAdCollapsed(ad: Ad) {
            }

            override fun onAdLoaded(ad: Ad) {
                // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                log(line = "onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Banner: onAdLoaded($ad)")
            }

            override fun onAdDisplayFailed(error: Throwable) {
                log(line = "onAdDisplayFailed: $error")
                println("MainActivity Banner: onAdDisplayed($error)")
            }

            override fun onAdClicked(ad: Ad) {
                log(line = "onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
                println("MainActivity Banner: onAdClicked($ad)")
            }
        })
    }

    private fun initApplovin() {
        AppLovinDecorator.getInstance(this).mediationProvider = "max"
        AppLovinDecorator
            .register(
//                BidMachineAdapter::class.java,
                AdmobAdapter::class.java
            ).initializeSdk(this) { appLovinSdkConfiguration ->
                println(appLovinSdkConfiguration)
                binding.initButton.isVisible = false
                log("Initialized")
            }

    }

    private fun log(line: String) {
        synchronized(this) {
            with(binding) {
                val text = logTextView.text.toString()
                logTextView.text = text + "\n\n" + line
                logTextView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }
}