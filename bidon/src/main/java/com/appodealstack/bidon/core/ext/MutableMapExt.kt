package com.appodealstack.bidon.core.ext

internal fun <K, V> MutableMap<K, V>.addOrRemoveIfNull(key: K, value: V?) {
    if (value != null) {
        this[key] = value
    } else {
        this.remove(key)
    }
}