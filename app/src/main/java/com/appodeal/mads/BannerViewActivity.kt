package com.appodeal.mads

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.appodealstack.bidon.ads.banner.BannerView

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
            bannerView.load()
            bannerView.show()
        }
        destroyButton.setOnClickListener {
            bannerView.destroy()
        }
        closeButton.setOnClickListener {
            onBackPressed()
        }
    }
}