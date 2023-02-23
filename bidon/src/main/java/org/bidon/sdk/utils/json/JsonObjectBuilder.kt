package org.bidon.sdk.utils.json

import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */

internal fun jsonObject(method: JsonObjectBuilder.() -> Unit): JSONObject {
    return JsonObjectBuilder().apply(method).build()
}

internal fun jsonObject(putTo: JSONObject, method: JsonObjectBuilder.() -> Unit): JSONObject {
    return JsonObjectBuilder(putTo).apply(method).build()
}

internal fun jsonArray(method: JsonArrayBuilder.() -> Unit): JSONArray {
    return JsonArrayBuilder().apply(method).build()
}

internal class JsonObjectBuilder(
    private val jsonObject: JSONObject = JSONObject()
) {
    /**
     * It removes key if value is null
     */
    infix fun String.hasValue(value: Any?) {
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

internal class JsonArrayBuilder {
    private val jsonArray = JSONArray()

    fun putValues(value: List<Any>?) {
        if (value == null) {
            return
        }
        value.forEach {
            jsonArray.put(it)
        }
    }

    fun build(): JSONArray {
        return jsonArray
    }
}