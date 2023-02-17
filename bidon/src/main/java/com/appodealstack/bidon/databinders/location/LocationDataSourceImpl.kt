package com.appodealstack.bidon.databinders.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Process
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import java.lang.ref.WeakReference
import java.util.*

internal class LocationDataSourceImpl(
    private val context: Context
) : LocationDataSource {

    private var weakLocationManager: WeakReference<LocationManager>? = null
    private val deviceLocation: Location? get() = getLocation(context)

    private val address by lazy {
        try {
            if (deviceLocation == null) return@lazy null
            val location = requireNotNull(deviceLocation)

            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            addresses?.first()
        } catch (e: Exception) {
            logError(Tag, "Error while retrieving location", e)
            null
        }
    }

    override fun getLatitude(): Double? = deviceLocation?.latitude
    override fun getLongitude(): Double? = deviceLocation?.longitude
    override fun getAccuracy(): Float? = deviceLocation?.accuracy
    override fun getLastFix(): Long? = deviceLocation?.time
    override fun getCountry(): String? = address?.countryCode
    override fun getRegion(): String? = address?.adminArea
    override fun getCity(): String? = address?.locality
    override fun getZip(): String? = address?.postalCode
    override fun getUtcOffset(): Int {
        val tz = TimeZone.getDefault()
        val now = Date()
        return tz.getOffset(now.time) / HourInMs
    }

    /**
     * Get location data using [LocationManager].
     *
     * @return [android.location.Location] or null
     */
    @SuppressLint("MissingPermission")
    private fun getLocation(context: Context): Location? {
        if (!isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            return null
        }
        val locationManager: LocationManager = getLocationManager(context)
        return locationManager.getBestProvider(Criteria(), false)?.let {
            try {
                locationManager.getLastKnownLocation(it).also {
                    logInfo(Tag, "Location $it")
                }
            } catch (e: SecurityException) {
                logError(Tag, "failed to retrieve GPS location: permission not granted", e)
                null
            } catch (e: IllegalArgumentException) {
                logError(Tag, "failed to retrieve GPS location: device has no GPS provider", e)
                null
            }
        }
    }

    /**
     * Get [LocationManager] from [WeakReference] or create new.
     *
     * @return [LocationManager].
     */
    private fun getLocationManager(context: Context): LocationManager {
        return weakLocationManager?.get() ?: run {
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).also {
                weakLocationManager = WeakReference(it)
            }
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkPermission(permission, Process.myPid(), Process.myUid())
    }
}

private const val Tag = "Location"
private const val HourInMs = 1000 * 60 * 60
