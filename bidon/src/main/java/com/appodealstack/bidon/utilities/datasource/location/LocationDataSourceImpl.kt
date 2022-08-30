package com.appodealstack.bidon.utilities.datasource.location

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Process
import com.appodealstack.bidon.core.ext.logInfo
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

internal class LocationDataSourceImpl(
    private val context: Context
) : LocationDataSource {

    private val locationType: Int?
    private var weakLocationManager: WeakReference<LocationManager>? = null

    private var countryId: String? = null
    private var city: String? = null
    private var zip: String? = null
    private var address: String? = null

    /**
     * @return current device location if permissions is granted. If it's not available by
     * restrictions, will return `null`.
     */
    private var deviceLocation: Location? = null
        get() = if (field == null) {
            lastLocation
        } else field

    override fun getAccuracy(): Float? {
        return deviceLocation?.accuracy
    }

    override fun getLastFix(): Long? {
        return deviceLocation?.time
    }

    override fun getCountry(): String? {
        return countryId
    }

    override fun getRegion(): String? {
        // TODO naming?
        return address
    }

    override fun getCity(): String? {
        return city
    }

    override fun getZip(): String? {
        return zip
    }

    override fun getUtcOffset(): Int {
        val tz = TimeZone.getDefault()
        val now = Date()
        return tz.getOffset(now.time) / 1000
    }

    override fun getLat(): Double? {
        return deviceLocation?.latitude
    }

    override fun getLon(): Double? {
        return deviceLocation?.longitude
    }

    /**
     * Get location data using [LocationManager].
     *
     * @return [android.location.Location] or null
     */
    @SuppressLint("MissingPermission")
    private fun getLocation(context: Context): Location? {
        if (!isPermissionGranted(context, permission.ACCESS_FINE_LOCATION) &&
            !isPermissionGranted(context, permission.ACCESS_COARSE_LOCATION)
        ) {
            return null
        }
        val locationManager: LocationManager = getLocationManager(context)
        val bestProvider = locationManager.getBestProvider(Criteria(), false)
        var location: Location? = null
        if (bestProvider != null) {
            try {
                location = locationManager.getLastKnownLocation(bestProvider)
            } catch (e: SecurityException) {
                logInfo(Tag, "failed to retrieve GPS location: permission not granted")
            } catch (e: IllegalArgumentException) {
                logInfo(Tag, "failed to retrieve GPS location: device has no GPS provider")
            }
        }
        return location
    }

    /**
     * Get [LocationManager] from [WeakReference] or create new.
     *
     * @return [LocationManager].
     */
    private fun getLocationManager(context: Context): LocationManager {
        var locationManager = weakLocationManager?.get()
        if (locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            weakLocationManager = WeakReference(locationManager)
        }
        return locationManager
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkPermission(permission, Process.myPid(), Process.myUid())
    }

    companion object {
        private const val COUNTRY_ID = "country_id"
        private const val ADDRESS = "address"
        private const val LAT = "lat"
        private const val LON = "lon"
        private const val CITY = "city"
        private const val ZIP = "zip"
        var lastLocation: Location? = null
        private const val Tag = "Location"
    }

    init {
        deviceLocation = getLocation(context)
        if (deviceLocation != null) {
            lastLocation = deviceLocation
        }
        locationType = if (deviceLocation == null) 0 else 1
    }

    fun parse(data: JSONObject?) {
        if (data == null) {
            return
        }
        val waterfallUserSettings = data.optJSONObject("user_settings")
        if (waterfallUserSettings != null) {
            var lat = -1.0
            var lon = -1.0
            if (waterfallUserSettings.has(LAT)) {
                lat = waterfallUserSettings.optDouble(LAT, -1.0)
            }
            if (waterfallUserSettings.has(LON)) {
                lon = waterfallUserSettings.optDouble(LON, -1.0)
            }
            if (lat > -1 && lon > -1) {
                lastLocation = Location(LocationManager.PASSIVE_PROVIDER).apply {
                    latitude = lat
                    longitude = lon
                }
            }
            city = getStringOrNullFromJson(
                waterfallUserSettings,
                CITY, "unknown"
            )
            zip = getStringOrNullFromJson(
                waterfallUserSettings,
                ZIP, "unknown"
            )
        }
        countryId = getStringOrNullFromJson(data, COUNTRY_ID, "unknown")
        address = getStringOrNullFromJson(data, ADDRESS, "unknown")
    }

    private fun getStringOrNullFromJson(
        jsonObject: JSONObject?,
        field: String?,
        fallback: String?
    ): String? {
        return if (jsonObject == null || field == null || jsonObject.isNull(field)) {
            fallback
        } else jsonObject.optString(field, fallback)
    }
}