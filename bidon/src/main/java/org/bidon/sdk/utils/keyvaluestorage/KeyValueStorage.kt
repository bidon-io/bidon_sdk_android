package org.bidon.sdk.utils.keyvaluestorage

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Add unique [Key] for each variable.
 *
 * Use it in background (!) thread for avoiding ANRs.
 */
internal interface KeyValueStorage {
    val applicationId: String // ID that app generates on the very first launch and send across session.
    var appKey: String?
    var token: String?
    var host: String?
    var segmentUid: String?

    fun clear()
}

internal enum class Key {
    Token,
    BidonAppKey,
    ClientApplicationId,
    Host,
    SegmentUid,
}
