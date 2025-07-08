package org.bidon.demoapp

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import org.bidon.sdk.ads.banner.BannerView

class BannerViewActivity : AppCompatActivity(R.layout.banner_view_layout) {

    private var bannerView: BannerView? = StaticBanner.bannerView

    //    private val bannerView by lazy {
//        findViewById<BannerView>(R.id.bannerView)
//    }
    private val bannerContainer by lazy {
        findViewById<FrameLayout>(R.id.bannerContainer)
    }

    private val createButton by lazy {
        findViewById<Button>(R.id.createButton)
    }

    private val loadButton by lazy {
        findViewById<Button>(R.id.loadButton)
    }

    private val showButton by lazy {
        findViewById<Button>(R.id.showButton)
    }

    private val destroyButton by lazy {
        findViewById<Button>(R.id.destroyButton)
    }

    private val closeButton by lazy {
        findViewById<TextView>(R.id.closeButton)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }

    private fun initViews() {
        createButton.setOnClickListener {
            bannerView = BannerView(this).also {
                StaticBanner.bannerView = it
            }
        }
        loadButton.setOnClickListener {
            bannerView?.loadAd(activity = this)
        }
        showButton.setOnClickListener {
            bannerView?.let {
                if (it !in bannerContainer.children) {
                    (bannerView?.parent as? ViewGroup)?.removeView(it)
                    bannerContainer.removeAllViews()
                    bannerContainer.addView(it)
                    it.showAd()
                }
            }
        }
        destroyButton.setOnClickListener {
            StaticBanner.bannerView = null
            bannerContainer.removeAllViews()
            bannerView?.destroyAd()
            bannerView = null
        }
        closeButton.setOnClickListener {
            StaticBanner.bannerView = null
            @Suppress("DEPRECATION")
            onBackPressed()
        }
    }
}

object StaticBanner {
    var bannerView: BannerView? = null
}