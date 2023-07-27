package org.bidon.sdk.databinders.extras

import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.json.JSONObject

internal class ExtrasImpl : Extras {

    private val extras = mutableMapOf<String, Any>()

    override fun addExtra(key: String, value: Any?) {
        if (value != null && value.isTypeSupported()) {
            if (extras[key] != value) {
                logInfo(TAG, "Extras updated: $extras")
                extras[key] = value
            }
        } else {
            extras.remove(key)
        }
    }

    override fun getExtras(): Map<String, Any> = extras.toMap()

    private fun Any.isTypeSupported(): Boolean {
        return (
            this is String ||
                this is Int ||
                this is Long ||
                this is Double ||
                this is Float ||
                this is Boolean ||
                this is Char ||
                this is JSONObject
            ).also {
            if (!it) {
                logError(TAG, "Type of $this is not supported", UnsupportedOperationException())
            }
        }
    }
}

private const val TAG = "Extras"