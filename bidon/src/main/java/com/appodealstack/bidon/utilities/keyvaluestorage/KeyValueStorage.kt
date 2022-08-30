package com.appodealstack.bidon.utilities.keyvaluestorage

import android.content.Context

/**
 * Add unique [Key] for each variable.
 */
interface KeyValueStorage {
    val appKey: String?
    var token: String?

    fun init(context: Context)
    fun clear()
}

internal enum class Key {
    Token, BidonAppKey
}

