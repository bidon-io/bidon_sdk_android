package com.appodeal.mads

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.appodeal.mads.databinding.ActivityMainBinding
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.admob.AdmobParameters
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.applovin.rewarded.BNMaxRewardedAd
import com.appodealstack.bidmachine.BidMachineAdapter
import com.appodealstack.bidmachine.BidMachineParameters
import com.appodealstack.mads.demands.banners.BannerSize

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val adBannerProgrammatically: BNMaxAdView by lazy {
        BNMaxAdView(
            adUnitId = "c7c5f664e60b9bfb",
            adFormat = BannerSize.Banner,
            context = this
        )
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

        interstitialAd.setInterstitialListener(::log)
        rewardedAd.setRewardedListener(::log)
        adBannerProgrammatically.setBannerListener(::log)
        binding.adBannerView.setBannerListener(::log)
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
//                adBannerProgrammatically.apply {
//                    // Stretch to the width of the screen for banners to be fully functional
//                    val width = ViewGroup.LayoutParams.MATCH_PARENT
//                    // Get the adaptive banner height.
//                    val heightDp = MaxAdFormat.BANNER.getAdaptiveSize(this@MainActivity).height
//                    val heightPx = AppLovinSdkUtils.dpToPx(this@MainActivity, heightDp)
//                    this.layoutParams = FrameLayout.LayoutParams(width, heightPx)
//                    this.setExtraParameter("adaptive_banner", "true")
//                }
//                adBannerProgrammatically.stopAutoRefresh()
                adBannerProgrammatically.startAutoRefresh()
                adBannerProgrammatically.loadAd()
            }
            loadBannerXmlButton.setOnClickListener {
                adBannerView.loadAd()
            }
            adLayout.addView(adBannerProgrammatically)
            hideBannerButton.setOnClickListener {
                adBannerView.destroy()
                adBannerProgrammatically.destroy()
            }
        }
    }

    private fun initApplovin() {
        AppLovinDecorator.getInstance(this).mediationProvider = "max"
        AppLovinDecorator
            .register(
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
            )
            .register(
                BidMachineAdapter::class.java,
                BidMachineParameters(sourceId = "1")
            )
            .initializeSdk(this) { appLovinSdkConfiguration ->
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