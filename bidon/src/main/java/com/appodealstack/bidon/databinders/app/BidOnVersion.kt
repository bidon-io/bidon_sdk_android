package com.appodealstack.bidon.databinders.app

import com.appodealstack.bidon.BuildConfig

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */

object BidOnVersion {
    var version: String = BuildConfig.ADAPTER_VERSION
    var frameworkName = "android"
    var frameworkVersion: String? = null
    var engineVersion: String? = null
}