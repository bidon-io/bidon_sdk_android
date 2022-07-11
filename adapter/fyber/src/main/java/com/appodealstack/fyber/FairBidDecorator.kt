package com.appodealstack.fyber

import android.app.Activity
import com.fyber.FairBid

object FairBidDecorator {
    fun start(appId: String, activity: Activity){
        FairBid.start(appId, activity)
    }
}