package org.bidon.sdk.utils.keyvaluestorage

import android.content.Context
import java.util.*
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class KeyValueStorageImpl(private val context: Context) : KeyValueStorage {
    private val sharedPreferences by lazy {
        context.getSharedPreferences("bidon_preferences", Context.MODE_PRIVATE)
    }
    override var token: String?
        get() = Key.Token.getString()
        set(value) = Key.Token.saveString(value)

    override var host: String?
        get() = Key.Host.getString()
        set(value) = Key.Host.saveString(value)

    override val applicationId: String
        get() = Key.ClientApplicationId.getString() ?: UUID.randomUUID().toString().also {
            Key.ClientApplicationId.saveString(it)
        }

    override var appKey: String?
        get() = Key.BidonAppKey.getString()
        set(value) = Key.BidonAppKey.saveString(value)

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