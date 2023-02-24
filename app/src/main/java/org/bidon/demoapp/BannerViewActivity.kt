package org.bidon.demoapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.bidon.sdk.ads.banner.BannerView

class BannerViewActivity : AppCompatActivity(R.layout.banner_view_layout) {

    private val bannerView by lazy {
        findViewById<BannerView>(R.id.bannerView)
    }

    private val loadButton by lazy {
        findViewById<Button>(R.id.loadButton)
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
        loadButton.setOnClickListener {
            bannerView.loadAd()
            bannerView.showAd()
        }
        destroyButton.setOnClickListener {
            bannerView.destroyAd()
        }
        closeButton.setOnClickListener {
            onBackPressed()
        }
    }
}