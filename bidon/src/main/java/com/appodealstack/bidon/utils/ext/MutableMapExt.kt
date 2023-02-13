package com.appodealstack.bidon.utils.ext

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal fun <K, V> MutableMap<K, V>.addOrRemoveIfNull(key: K, value: V?) {
    if (value != null) {
        this[key] = value
    } else {
        this.remove(key)
    }
}
