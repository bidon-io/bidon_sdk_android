package com.appodealstack.mads.demands

import android.app.Activity
import android.os.Bundle

interface AdProvider {
    fun canShow(): Boolean
    fun showAd(activity: Activity?, adParams: Bundle)
    fun destroy()
}