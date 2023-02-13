package com.appodealstack.bidon.databinders.app

import com.appodealstack.bidon.databinders.DataSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AppDataSource : DataSource {
    fun getBundleId(): String
    fun getVersionName(): String?
    fun getVersionCode(): Number?
    fun getAppKey(): String?
    fun getFramework(): String
    fun getFrameworkVersion(): String?
    fun getPluginVersion(): String?
    fun getVersion(): String
}