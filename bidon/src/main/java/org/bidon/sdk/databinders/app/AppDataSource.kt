package org.bidon.sdk.databinders.app

import org.bidon.sdk.databinders.DataSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * [Scheme](https://appodeal.atlassian.net/wiki/spaces/SX/pages/4490264831/SDK+Server+Schema#SDK%3C%3EServerSchema-App)
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