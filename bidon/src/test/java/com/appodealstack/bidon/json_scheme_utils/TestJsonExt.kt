package com.appodealstack.bidon.json_scheme_utils

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.assertEqualsTo(other: JSONObject) {
    val actual = JSONObject(this.toString())
    val expect = JSONObject(other.toString())

    val actualKeys = mutableListOf<String>().apply {
        actual.keys().forEach { this.add(it) }
    }
    val expectedKeys = mutableListOf<String>().apply {
        expect.keys().forEach { this.add(it) }
    }
    if (!actualKeys.containsAll(expectedKeys)) {
        val diff = expectedKeys - actualKeys
        error("'expect' has extra keys: $diff, but 'actual' doesn't.\nexpect: $expect\nactual: $actual")
    }
    if (!expectedKeys.containsAll(actualKeys)) {
        val diff = actualKeys - expectedKeys
        error("'actual' has extra keys: $diff, but 'expect' doesn't.\nexpect: $expect\nactual: $actual")
    }
    actualKeys.forEach { key ->
        val actualValue = actual.get(key)
        val expectValue = expect.get(key)
        when (getWhateverValue(expect.optString(key))) {
            Whatever.String -> {
                try {
                    actual.getString(key)
                } catch (e: Exception) {
                    error("Expected any String, but $actualValue found.\nexpect: $expect\nactual: $actual")
                }
            }
            Whatever.Boolean -> {
                requireNotNull(actual.optBoolean(key)) {
                    "Expected any Boolean, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            Whatever.Int -> {
                requireNotNull(actual.optInt(key)) {
                    "Expected any Int, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            Whatever.Long -> {
                requireNotNull(actual.optLong(key)) {
                    "Expected any Long, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            Whatever.Double -> {
                requireNotNull(actual.optDouble(key)) {
                    "Expected any Double, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            Whatever.Json -> {
                requireNotNull(actual.optJSONObject(key)) {
                    "Expected any JSONObject, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            Whatever.JsonArray -> {
                requireNotNull(actual.optJSONArray(key)) {
                    "Expected any optJSONArray, but $actualValue found.\nexpect: $expect\nactual: $actual"
                }
            }
            null -> {
                when (actualValue) {
                    is JSONArray -> {
                        if (expectValue is JSONArray) {
                            actualValue.assertEqualsTo(expectValue)
                        } else {
                            error("'expect' key: $key doesn't have a JSONArray-value as 'actual' does.\nexpect: $expect\nactual: $actual")
                        }
                    }
                    is JSONObject -> {
                        if (expectValue is JSONObject) {
                            actualValue.assertEqualsTo(expectValue)
                        } else {
                            error("'expect' key: $key doesn't have a JSONObject-value as 'actual' does.\nexpect: $expect\nactual: $actual")
                        }
                    }
                    else -> {
                        if (actualValue != expectValue) {
                            error("Expected $expectValue, but $actualValue found.\nexpect: $expect\nactual: $actual")
                        }
                    }
                }
            }
        }
        expect.remove(key)
        actual.remove(key)
    }
}

internal fun JSONArray.assertEqualsTo(other: JSONArray) {
    val actual = this
    val expect = other
    val count = actual.length()
    if (actual.length() != expect.length()) {
        error("'expect' array size: ${expect.length()}, but 'actual' size: ${actual.length()}.\nexpect: $expect\nactual: $actual")
    }
    if (count == 0) return

    try {
        actual.getJSONObject(0).assertEqualsTo(expect.getJSONObject(0))
        return
    } catch (_: Exception) {
    }
    try {
        actual.getJSONArray(0).assertEqualsTo(expect.getJSONArray(0))
        return
    } catch (_: Exception) {
    }
    try {
        actual.getBoolean(0)
        val set = mutableSetOf<Boolean>()
        repeat(count) {
            set.add(actual.getBoolean(it))
        }
        repeat(count) {
            set.remove(expect.getBoolean(it))
        }
        if (set.isNotEmpty()) {
            error("expect: $expect\nactual: $actual")
        }
        return
    } catch (_: Exception) {
    }
    try {
        actual.getString(0)
        val set = mutableSetOf<String>()
        repeat(count) {
            set.add(actual.getString(it))
        }
        repeat(count) {
            set.remove(expect.getString(it))
        }
        if (set.isNotEmpty()) {
            error("expect: $expect\nactual: $actual")
        }
        return
    } catch (_: Exception) {
    }
    try {
        actual.getLong(0)
        val set = mutableSetOf<Long>()
        repeat(count) {
            set.add(actual.getLong(it))
        }
        repeat(count) {
            set.remove(expect.getLong(it))
        }
        if (set.isNotEmpty()) {
            error("expect: $expect\nactual: $actual")
        }
        return
    } catch (_: Exception) {
    }

    try {
        actual.getDouble(0)
        val set = mutableSetOf<Double>()
        repeat(count) {
            set.add(actual.getDouble(it))
        }
        repeat(count) {
            set.remove(expect.getDouble(it))
        }
        if (set.isNotEmpty()) {
            error("expect: $expect\nactual: $actual")
        }
        return
    } catch (_: Exception) {
    }

    error("Cannot check.\nexpect: $expect\nactual: $actual")
}

internal fun formatWhatever(value: String) = "_Whatever_${value.uppercase()}_"

private fun getWhateverValue(key: String) = Whatever.values().firstOrNull { key == formatWhatever(it.name) }
