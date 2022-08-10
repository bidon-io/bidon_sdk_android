package com.appodealstack.bidon.adapters

import android.app.Activity
import android.os.Bundle
import android.view.View

interface AdProvider {
    fun canShow(): Boolean
    fun showAd(activity: Activity?, adParams: Bundle)
    fun destroy()
}

interface AdViewProvider {
    fun getAdView(): View
}