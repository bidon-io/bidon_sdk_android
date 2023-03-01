package org.bidon.sdk.databinders.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage

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
    override fun getFramework(): String = BidonVersion.frameworkName
    override fun getFrameworkVersion(): String? = BidonVersion.frameworkVersion
    override fun getPluginVersion(): String? = BidonVersion.engineVersion
    override fun getVersion(): String = BidonVersion.version

    private fun getPackageInfo(context: Context): PackageInfo? {
        var packageInfo: PackageInfo? = null
        try {
            @Suppress("DEPRECATION")
            packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (throwable: Throwable) {
            logError(Tag, message = throwable.message ?: "", error = throwable)
        }
        return packageInfo
    }
}

private const val Tag = "AppDataSource"