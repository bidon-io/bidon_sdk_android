package com.appodealstack.bidon.core

internal enum class UrlPathAdType(val lastSegment: String) {
    Banner("banner"),
    Interstitial("interstitial"),
    Rewarded("rewarded_video"),
}