package com.appodealstack.bidon.utilities

import org.json.JSONArray
import org.json.JSONObject

fun <T> JSONArray?.asList(): List<T> {
    if (this == null || this.length() == 0) return emptyList()
    val list = mutableListOf<T>()
    for (i in 0 until length()) {
        (opt(i) as? T)?.let { list.add(it) }
    }
    return list
}

fun <T> JSONObject?.toMap(): Map<String, T> {
    if (this == null || this.length() == 0) return emptyMap()
    val map: MutableMap<String, T> = mutableMapOf()
    val keysIterator = keys()
    while (keysIterator.hasNext()) {
        val key = keysIterator.next()
        map[key] = opt(key) as T
    }
    return map
}