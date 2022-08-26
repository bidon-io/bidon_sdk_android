package com.appodealstack.bidon.utilities.datasource.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.appodealstack.bidon.Version
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.utilities.datasource.DataSource
import com.appodealstack.bidon.utilities.datasource.SourceType
import com.appodealstack.bidon.utilities.datasource.SourceType.App

internal class AppDataSourceImpl(
    private val contextProvider: ContextProvider
) : AppDataSource {

    val context: Context
    get() = contextProvider.requiredContext

    override fun getBundleId(): String {
        return context.packageName
    }

    override fun getVersionName(): String? {
        return getPackageInfo(context)?.versionName
    }

    override fun getVersionCode(): Number? {
        val packageInfo = getPackageInfo(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode
        } else {
            packageInfo?.versionCode
        }
    }

    override fun getAppKey(): String? {
        TODO("SharedPreferences")
        return null
    }

    override fun getFramework(): String {
        return Version.frameworkName
    }

    override fun getFrameworkVersion(): String? {
        return Version.frameworkVersion
    }

    override fun getPluginVersion(): String? {
        return Version.engineVersion
    }

    override fun getVersion(): String {
        return Version.version
    }

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