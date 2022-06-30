package com.appodealstack.mads.base.ext

internal fun <K, V> MutableMap<K, V>.addOrRemoveNull(key: K, value: V?) {
    if (value != null) {
        this[key] = value
    } else {
        this.remove(key)
    }
}