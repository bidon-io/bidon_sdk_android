package com.appodealstack.bidon.data.keyvaluestorage

/**
 * Add unique [Key] for each variable.
 *
 * Use it in background (!) thread for avoiding ANRs.
 */
interface KeyValueStorage {
    val applicationId: String // ID that app generates on the very first launch and send across session.
    var appKey: String?
    var token: String?
    var host: String?

    fun clear()
}

internal enum class Key {
    Token,
    BidonAppKey,
    ClientApplicationId,
    Host,
}
