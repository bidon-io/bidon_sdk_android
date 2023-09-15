package org.bidon.sdk.utils.serializer

import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.json.jsonArray
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

internal object BidonSerializer {
    fun serializeToArray(data: List<Any>): JSONArray {
        return jsonArray {
            putValues(data.map { serialize(it) })
        }
    }

    fun serialize(data: Any): JSONObject {
        return jsonObject {
            data.getSerialParams().forEach { field ->
                field.fieldName hasValue when (field.value) {
                    null -> null
                    is Serializable -> serialize(field.value)
                    is String -> field.value
                    is Double -> field.value
                    is Int -> field.value
                    is Long -> field.value
                    is String -> field.value
                    is Float -> field.value
                    is Boolean -> field.value
                    is Char -> field.value
                    is List<*> -> {
                        jsonArray {
                            val array = field.value.mapNotNull { value ->
                                when (value) {
                                    null -> null
                                    is Serializable -> serialize(value)
                                    is String -> value
                                    is Double -> value
                                    is Int -> value
                                    is Long -> value
                                    is String -> value
                                    is Float -> value
                                    is Boolean -> value
                                    is Char -> value
                                    else -> {
                                        logFailure(data, field)
                                        null
                                    }
                                }
                            }
                            putValues(array)
                        }
                    }

                    is Map<*, *> -> {
                        jsonObject {
                            field.value.forEach { (key, value) ->
                                if (key != null) {
                                    require(key is String) {
                                        "key is not String type: key=($key) is ${key::class.java})"
                                    }
                                    key hasValue when (value) {
                                        null -> null
                                        is Serializable -> serialize(value)
                                        is String -> value
                                        is Double -> value
                                        is Int -> value
                                        is Long -> value
                                        is String -> value
                                        is Float -> value
                                        is Boolean -> value
                                        is Char -> value
                                        else -> {
                                            logFailure(data, field)
                                            null
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        logFailure(data, field)
                        null
                    }
                }
            }
        }
    }

    private fun Any.getSerialParams(): List<SerialParams> {
        return this::class.declaredMemberProperties.mapNotNull { field ->
            if (field.javaField?.isAnnotationPresent(JsonName::class.java) == true) {
                val annotation = field.javaField?.getAnnotation(JsonName::class.java)
                if (annotation != null) {
                    SerialParams(
                        fieldName = annotation.key,
                        value = readInstanceProperty(this, field.name)
                    )
                } else {
                    logError(TAG, "No annotation @SerialName set to field: ${field.name}", Serializable.Error.NotAnnotatedField)
                    null
                }
            } else {
                null
            }
        }
    }

    private fun logFailure(data: Any, field: SerialParams) {
        logError(TAG, "Error while serializing: $data. Field: $field", Serializable.Error.UnknownClass)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R> readInstanceProperty(instance: Any, propertyName: String): R? {
        val property = instance::class.members
            // don't cast here to <Any, R>, it would succeed silently
            .first { it.name == propertyName } as KProperty1<Any, *>
        // force a invalid cast exception if incorrect type here
        return property.get(instance) as? R
    }

    class SerialParams(
        val fieldName: String,
        val value: Any?,
    )
}

private const val TAG = "BidonSerializer"