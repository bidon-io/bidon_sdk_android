package org.bidon.sdk.databinders.app

import org.bidon.sdk.BuildConfig

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */

object BidonVersion {
    var version: String = BuildConfig.ADAPTER_VERSION
    var frameworkName = "android"
    var frameworkVersion: String? = null
    var engineVersion: String? = null
}