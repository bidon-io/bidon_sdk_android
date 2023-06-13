package org.bidon.sdk.databinders.app

import org.bidon.sdk.databinders.DataSource

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface AppDataSource : DataSource {
    /**
     * Application ID
     */
    fun getBundleId(): String

    /**
     * Application versionName
     */
    fun getAppVersionName(): String?

    /**
     * Application versionCode
     */
    fun getAppVersionCode(): Number?

    /**
     * Bidon APP_KEY
     */
    fun getAppKey(): String?

    /**
     * Framework name: android/unity
     */
    fun getFramework(): String

    /**
     * Unity version
     */
    fun getFrameworkVersion(): String?

    /**
     * Unity Bidon plugin version
     */
    fun getPluginVersion(): String?

    /**
     * Bidon SDK version
     */
    fun getBidonVersion(): String
}