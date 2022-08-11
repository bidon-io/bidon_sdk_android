package com.appodealstack.bidon.config.domain.databinders

import android.content.Context
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import android.view.Display
import android.view.WindowManager
import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.config.domain.databinders.DeviceBinder.Device.ConnectionType
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ContextProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.*


internal class DeviceBinder(
    private val contextProvider: ContextProvider
) : DataBinder {
    override val fieldName: String = "device"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createDevice())

    private fun createDevice(): Device {
        val context = contextProvider.requiredContext
        val screenSize = getScreenSize(context)
        val metrics = context.resources.displayMetrics
        return Device(
            userAgent = "=-=-=", //TODO a28 obtain user agent
            manufacturer = Build.MANUFACTURER,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            hardwareVersion = Build.HARDWARE,
            width = screenSize.x,
            height = screenSize.y,
            ppi = metrics.densityDpi,
            pxRatio = metrics.density,
            javaScriptSupport = 1,  //TODO a28 obtain javaScriptSupport. 1- supports, 0 - no
            language = Locale.getDefault().toString(),
            carrier = getCarrier(context),
            mccmnc = getPhoneMCCMNC(context),
            connectionType = getConnectionType(context).code
        )
    }

    @Serializable
    data class Device(
        @SerialName("ua")
        val userAgent: String?,
        @SerialName("make")
        val manufacturer: String?,
        @SerialName("model")
        val deviceModel: String?,
        @SerialName("os")
        val os: String?,
        @SerialName("osv")
        val osVersion: String?,
        @SerialName("hwv")
        val hardwareVersion: String?,

        @SerialName("h")
        val height: Int?,
        @SerialName("w")
        val width: Int?,
        @SerialName("ppi")
        val ppi: Int?,

        @SerialName("pxratio")
        val pxRatio: Float?,
        @SerialName("js")
        val javaScriptSupport: Int?,

        @SerialName("language")
        val language: String?,
        @SerialName("carrier")
        val carrier: String?,
        @SerialName("mccmnc")
        val mccmnc: String?,
        @SerialName("connection_type")
        val connectionType: Int?,
    ) {
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

    /**
     * Get mobile operator data using [TelephonyManager].
     *
     * @return the numeric name of current registered operator.
     */
    private fun getCarrier(context: Context): String? {
        val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkOperator = tel.networkOperator
        return if (networkOperator != null && networkOperator.length >= 3) {
            networkOperator.substring(0, 3) + '-' + networkOperator.substring(3)
        } else {
            null
        }
    }

    private fun getPhoneMCCMNC(context: Context): String? {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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

    private fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo = connectivityManager.activeNetworkInfo ?: return ConnectionType.Invalid
        return when (networkInfo.type) {
            ConnectivityManager.TYPE_MOBILE -> getMobileNetworkType(networkInfo)
            ConnectivityManager.TYPE_WIFI -> ConnectionType.WiFI
            ConnectivityManager.TYPE_ETHERNET -> ConnectionType.Ethernet
            else -> ConnectionType.Invalid
        }
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
}