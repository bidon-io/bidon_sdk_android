package com.appodealstack.mads.demands

import android.app.Activity
import android.os.Bundle

interface ObjRequest {
    fun canShowAd(): Boolean
    fun showAd(activity: Activity?, adParams: Bundle)
}