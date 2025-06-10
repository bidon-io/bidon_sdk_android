package com.applovin.mediation.adapters.ext

import android.os.Bundle

internal fun Bundle.getAsDouble(key: String, defaultValue: Double = 0.0): Double {
    return try {
        val value = this.get(key)
        if (value is Number) value.toDouble() else defaultValue
    } catch (_: Exception) {
        defaultValue
    }
}
