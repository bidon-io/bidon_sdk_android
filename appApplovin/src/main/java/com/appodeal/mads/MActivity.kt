package com.appodeal.mads

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.mads.demands.Ad

class MainActivity : FragmentActivity() {
    private val bannerListener = object : BNMaxAdViewAdListener {
        override fun onAdExpanded(ad: Ad) {}
        override fun onAdCollapsed(ad: Ad) {}
        override fun onAdLoaded(ad: Ad) {}
        override fun onAdDisplayFailed(error: Throwable) {}
        override fun onAdClicked(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bnMaxAdView = findViewById<BNMaxAdView>(R.id.bannerAdView)
        bnMaxAdView.setListener(bannerListener)

        bnMaxAdView.loadAd()
    }
}