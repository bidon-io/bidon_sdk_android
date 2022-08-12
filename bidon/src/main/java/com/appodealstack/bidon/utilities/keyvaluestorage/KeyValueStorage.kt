package com.appodealstack.bidon.utilities.keyvaluestorage

import android.content.Context

/**
 * Add unique [Key] for each variable.
 */
interface KeyValueStorage {
    var host: String?
    var port: Int?

    fun clear()
}

private enum class Key {
    Host, Port
}

class KeyValueStorageImpl(
    private val context: Context,
) : KeyValueStorage {
    private val sharedPreferences by lazy {
        context.getSharedPreferences("bidon_preferences", Context.MODE_PRIVATE)
    }

    override var host: String?
        get() = Key.Host.getString()
        set(value) = Key.Host.saveString(value)

    override var port: Int?
        get() = Key.Port.getInt()
        set(value) = Key.Port.saveInt(value ?: 0)

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * private
     */
    private fun Key.saveInt(value: Int) {
        sharedPreferences.edit().putInt(this.asKeyName(), value).apply()
    }

    private fun Key.getInt(): Int {
        return sharedPreferences.getInt(this.asKeyName(), 0)
    }

    private fun Key.saveString(value: String?) {
        sharedPreferences.edit().putString(this.asKeyName(), value).apply()
    }

    private fun Key.getString(): String? {
        return sharedPreferences.getString(this.asKeyName(), null)
    }

    private fun Key.asKeyName() = "${this.name}_KEY"
}