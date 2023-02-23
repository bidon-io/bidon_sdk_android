package com.appodealstack.bidon.json_scheme_utils

import org.json.JSONObject

@JvmInline
value class TestJson(val value: JSONObject)

fun JSONObject.assertEquals(expected: TestJson) {
    JSONObject(this.toString()).assertEqualsTo(expected.value)
}

fun expectedJsonStructure(method: JsonTestBuilder.() -> Unit) = TestJson(JsonTestBuilder().apply(method).build())

/**
 * If it doesn't matter what the value is, but Type does.
 */
enum class Whatever {
    String,
    Boolean,
    Int,
    Long,
    Double,
    Json,
    JsonArray;

    override fun toString() = formatWhatever(name)
}