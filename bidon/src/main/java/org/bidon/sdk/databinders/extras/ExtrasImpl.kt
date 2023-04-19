package org.bidon.sdk.databinders.extras

import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

internal class ExtrasImpl : Extras {

    private val extras = mutableMapOf<String, Any>()

    override fun addExtra(key: String, value: Any?) {
        if (value != null && value.isTypeSupported()) {
            extras[key] = value
        } else {
            extras.remove(key)
        }
        logInfo(Tag, "Extras updated: $extras")
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
                this is Char
            ).also {
            if (!it) {
                logError(Tag, "Type of $this is not supported", UnsupportedOperationException())
            }
        }
    }
}

private const val Tag = "Extras"