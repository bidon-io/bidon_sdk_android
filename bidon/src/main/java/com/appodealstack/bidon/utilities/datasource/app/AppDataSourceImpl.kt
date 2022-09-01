package com.appodealstack.bidon.utilities.datasource.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.appodealstack.bidon.Version
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage

internal class AppDataSourceImpl(
    private val context: Context,
    private val keyValueStorage: KeyValueStorage
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
    override fun getFramework(): String = Version.frameworkName
    override fun getFrameworkVersion(): String? = Version.frameworkVersion
    override fun getPluginVersion(): String? = Version.engineVersion
    override fun getVersion(): String = Version.version

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