package org.bidon.sdk.config.models.json_scheme_utils

import org.json.JSONArray
import org.json.JSONObject

class JsonTestBuilder(
    private val jsonObject: JSONObject = JSONObject()
) {

    infix fun String.has(value: Whatever) {
        jsonObject.put(this, value.toString())
    }

    /**
     * It removes key if value is null
     */
    infix fun String.hasJson(value: TestJson?) {
        if (value == null) {
            if (jsonObject.has(this)) {
                jsonObject.remove(this)
            }
            return
        }
        jsonObject.put(this, value.value)
    }

    /**
     * It removes key if value is null
     */
    infix fun String.hasArray(value: JSONArray?) {
        if (value == null) {
            if (jsonObject.has(this)) {
                jsonObject.remove(this)
            }
            return
        }
        jsonObject.put(this, value)
    }

    /**
     * To add JSONObject use [hasJson].
     * To add JSONArray use [hasArray].
     * It removes key if value is null
     */
    infix fun String.hasValue(value: Any?) {
        require(value !is JSONObject) {
            "Use [hasObject] instead"
        }
        require(value !is JSONArray) {
            "Use [hasArray] instead"
        }
        if (value == null) {
            if (jsonObject.has(this)) {
                jsonObject.remove(this)
            }
            return
        }
        jsonObject.put(this, value)
    }

    fun build(): JSONObject {
        return jsonObject
    }
}