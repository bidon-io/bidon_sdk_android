package org.bidon.sdk.databinders.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import org.bidon.sdk.BuildConfig
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class AppDataSourceImpl(
    private val context: Context,
    private val keyValueStorage: KeyValueStorage,
) : AppDataSource {

    override fun getAppVersionCode(): Number? {
        val packageInfo = getPackageInfo(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode
        }
    }

    override fun getBundleId(): String = context.packageName
    override fun getAppVersionName(): String? = getPackageInfo(context)?.versionName
    override fun getAppKey(): String? = keyValueStorage.appKey
    override fun getBidonVersion(): String = BuildConfig.ADAPTER_VERSION
    override fun getFramework(): String = UnitySpecificInfo.frameworkName
    override fun getFrameworkVersion(): String? = UnitySpecificInfo.frameworkVersion
    override fun getPluginVersion(): String? = UnitySpecificInfo.pluginVersion

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