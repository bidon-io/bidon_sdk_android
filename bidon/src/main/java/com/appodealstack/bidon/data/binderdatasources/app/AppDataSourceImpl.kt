package com.appodealstack.bidon.data.binderdatasources.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.appodealstack.bidon.BidONVersion
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.domain.stats.impl.logInternal

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AppDataSourceImpl(
    private val context: Context,
    private val keyValueStorage: KeyValueStorage,
) : AppDataSource {

    override fun getVersionCode(): Number? {
        val packageInfo = getPackageInfo(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode
        }
    }

    override fun getBundleId(): String = context.packageName
    override fun getVersionName(): String? = getPackageInfo(context)?.versionName
    override fun getAppKey(): String? = keyValueStorage.appKey
    override fun getFramework(): String = BidONVersion.frameworkName
    override fun getFrameworkVersion(): String? = BidONVersion.frameworkVersion
    override fun getPluginVersion(): String? = BidONVersion.engineVersion
    override fun getVersion(): String = BidONVersion.version

    private fun getPackageInfo(context: Context): PackageInfo? {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (throwable: Throwable) {
            logInternal(message = throwable.message ?: "", error = throwable)
        }
        return packageInfo
    }
}