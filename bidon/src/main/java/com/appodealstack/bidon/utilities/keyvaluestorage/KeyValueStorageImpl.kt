package com.appodealstack.bidon.utilities.keyvaluestorage

import android.content.Context

class KeyValueStorageImpl : KeyValueStorage {
    private var context: Context? = null

    private val sharedPreferences by lazy {
        requireNotNull(context) {
            "KeyValueStorage is not initialized"
        }.getSharedPreferences("bidon_preferences", Context.MODE_PRIVATE)
    }
    override var token: String?
        get() = Key.Token.getString()
        set(value) = Key.Token.saveString(value)

    override var appKey: String?
        get() = Key.BidonAppKey.getString()
        set(value) = Key.BidonAppKey.saveString(value)

    override fun init(context: Context) {
        this.context = context
    }

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