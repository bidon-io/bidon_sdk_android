package com.appodealstack.bidon.utilities.datasource.device

import android.content.Context
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import android.view.Display
import android.view.WindowManager
import android.webkit.WebSettings
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.utilities.datasource.DataSource
import com.appodealstack.bidon.utilities.datasource.SourceType
import com.appodealstack.bidon.utilities.restricted.Restrictable
import java.util.*

class DeviceDataSourceImpl(
    private val contextProvider: ContextProvider,
) : DeviceDataSource {

    private var cachedHttpAgentString: String? = null
    private val model: String = Build.MODEL
    private val buildId: String = Build.ID
    private val versionRelease: String = Build.VERSION.RELEASE
    private val context: Context
        get() = contextProvider.requiredContext
    private val screenSize by lazy {
        getScreenSize(context)
    }
    private val metrics by lazy {
        context.resources.displayMetrics
    }

    enum class ConnectionType(val code: Int) {
        Invalid(0),
        Ethernet(1),
        WiFI(2),
        CellularUnknown(3),
        Cellular2G(4),
        Cellular3G(5),
        Cellular4G(6),
        Cellular5G(7),
    }

    override fun getUserAgent(): String? {
        if (cachedHttpAgentString != null) {
            return cachedHttpAgentString
        }
        try {
            cachedHttpAgentString = WebSettings.getDefaultUserAgent(context)
        } catch (throwable: Throwable) {
            logInternal(message = throwable.message ?: "", error = throwable)
        }
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = generateHttpAgentString(context)
        }
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = getSystemHttpAgentString()
        }
        // We shouldn't try to obtain http agent string again after all possible methods has failed
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = ""
        }
        return cachedHttpAgentString
    }

    override fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    override fun getDeviceModel(): String {
        return "${getManufacturer()} $model"
    }

    override fun getOs(): String {
        return "Android"
    }

    override fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    override fun getHardwareVersion(): String {
        return Build.HARDWARE
    }

    override fun getScreenWidth(): Int {
        return screenSize.x
    }

    override fun getScreenHeight(): Int {
        return screenSize.y
    }

    override fun getPpi(): Int {
        return metrics.densityDpi
    }

    override fun getPxRatio(): Float {
        return metrics.density
    }

    override fun getJavaScriptSupport(): Int {
        return 1  //TODO a28 obtain javaScriptSupport. 1- supports, 0 - no
    }

    override fun getLanguage(): String {
        return Locale.getDefault().toString()
    }

    /**
     * Get mobile operator data using [TelephonyManager].
     *
     * @return the numeric name of current registered operator.
     */
    override fun getCarrier(): String? {
        val tel =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkOperator = tel.networkOperator
        return if (networkOperator != null && networkOperator.length >= 3) {
            networkOperator.substring(0, 3) + '-' + networkOperator.substring(3)
        } else {
            null
        }
    }

    override fun getPhoneMCCMNC(): String? {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator != null && networkOperator.length >= 3) {
                return String.format(
                    "%s-%s",
                    networkOperator.substring(0, 3),
                    networkOperator.substring(3)
                )
            }
        } catch (e: Exception) {
        }
        return null
    }

    override fun getConnectionTypeCode(): Int {
        return getConnectionType(context).code
    }

    private fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo =
            connectivityManager.activeNetworkInfo ?: return ConnectionType.Invalid
        return when (networkInfo.type) {
            ConnectivityManager.TYPE_MOBILE -> getMobileNetworkType(networkInfo)
            ConnectivityManager.TYPE_WIFI -> ConnectionType.WiFI
            ConnectivityManager.TYPE_ETHERNET -> ConnectionType.Ethernet
            else -> ConnectionType.Invalid
        }
    }

    /**
     * Get screen size using [WindowManager].
     *
     * @return screen size like pair width and height in pixels.
     */
    private fun getScreenSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    private fun getMobileNetworkType(networkInfo: NetworkInfo): ConnectionType {
        return when (networkInfo.subtype) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> ConnectionType.CellularUnknown
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> ConnectionType.Cellular2G

            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> ConnectionType.Cellular3G

            TelephonyManager.NETWORK_TYPE_NR -> ConnectionType.Cellular5G
            else -> ConnectionType.Cellular4G
        }
    }

    private fun generateHttpAgentString(context: Context): String? {
        return try {
            val builder = StringBuilder("Mozilla/5.0")
            builder.append(" (Linux; Android ")
                .append(versionRelease)
                .append("; ")
                .append(model)
                .append(" Build/")
                .append(buildId)
                .append("; wv)")
            // This AppleWebKit version supported from Chrome 68, and it's probably should for for
            // most devices
            builder.append(" AppleWebKit/537.36 (KHTML, like Gecko)")
            // This version is provided starting from Android 4.0
            builder.append(" Version/4.0")
            val pm = context.packageManager
            try {
                val pi = pm.getPackageInfo("com.google.android.webview", 0)
                builder.append(" Chrome/").append(pi.versionName)
            } catch (throwable: Throwable) {
                logInternal(message = throwable.message ?: "", error = throwable)
            }
            builder.append(" Mobile")
            try {
                val appInfo = context.applicationInfo
                val packageInfo = pm.getPackageInfo(context.packageName, 0)
                builder.append(" ")
                    .append(
                        if (appInfo.labelRes == 0) appInfo.nonLocalizedLabel.toString() else context.getString(
                            appInfo.labelRes
                        )
                    )
                    .append("/")
                    .append(packageInfo.versionName)
            } catch (throwable: Throwable) {
                logInternal(message = throwable.message ?: "", error = throwable)
            }
            builder.toString()
        } catch (throwable: Throwable) {
            logInternal(message = throwable.message ?: "", error = throwable)
            null
        }
    }

    private fun getSystemHttpAgentString(): String? {
        var result: String? = null
        try {
            result = System.getProperty("http.agent", "")
        } catch (throwable: Throwable) {
            logInternal(message = throwable.message ?: "", error = throwable)
        }
        return result
    }
}