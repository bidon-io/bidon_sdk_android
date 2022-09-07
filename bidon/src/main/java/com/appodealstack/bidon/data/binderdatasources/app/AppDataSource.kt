package com.appodealstack.bidon.data.binderdatasources.app

import com.appodealstack.bidon.data.binderdatasources.DataSource

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