package com.appodealstack.mads.auctions

import android.os.Bundle

interface ObjRequest {
    fun canShowAd(): Boolean
    fun showAd(adParams: Bundle)
}