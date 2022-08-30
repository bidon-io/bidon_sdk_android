package com.appodealstack.bidon.utilities.keyvaluestorage

/**
 * Add unique [Key] for each variable.
 *
 * Use it in non-Main thread for avoiding ANRs.
 */
interface KeyValueStorage {
    var appKey: String?
    var token: String?
    var host: String?

    fun clear()
}

internal enum class Key {
    Token, BidonAppKey, Host
}
