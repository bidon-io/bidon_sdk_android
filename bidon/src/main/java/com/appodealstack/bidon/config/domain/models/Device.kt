package com.appodealstack.bidon.config.domain.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userAgent)
        parcel.writeString(manufacturer)
        parcel.writeString(deviceModel)
        parcel.writeString(os)
        parcel.writeString(osVersion)
        parcel.writeString(hardwareVersion)
        parcel.writeValue(height)
        parcel.writeValue(width)
        parcel.writeValue(ppi)
        parcel.writeValue(pxRatio)
        parcel.writeValue(javaScriptSupport)
        parcel.writeString(language)
        parcel.writeString(carrier)
        parcel.writeString(mccmnc)
        parcel.writeValue(connectionType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Device> {
        override fun createFromParcel(parcel: Parcel): Device {
            return Device(parcel)
        }

        override fun newArray(size: Int): Array<Device?> {
            return arrayOfNulls(size)
        }
    }
}