package com.appodealstack.bidon.utils.serializer

import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.utils.json.jsonArray
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

/**
 * Created by Aleksei Cherniaev on 14/02/2023.
 */

interface Serializable

@Target(AnnotationTarget.FIELD)
annotation class JsonFieldName(val key: String)

internal interface Serializer {
    fun <T : Any> serialize(data: T): String
    fun <T : Any> parse(data: String?): T?
}

internal object BidonSerializer {
    inline fun <reified T : Serializable> parse(data: String?): T? = runCatching {
        if (data == null) return@runCatching null
        val json = JSONObject(data)
        val primaryConstructor = T::class.primaryConstructor!!

        val args = T::class.getParseParams().associate { parseParams ->
            val type = parseParams.parameter.type.toString()
            val value = when {
                type == "kotlin.String" -> json.getString(parseParams.fieldName)
                type == "kotlin.Double" -> json.getDouble(parseParams.fieldName)
                type == "kotlin.Int" -> json.getInt(parseParams.fieldName)
                type == "kotlin.Float" -> json.getDouble(parseParams.fieldName).toFloat()
                type == "kotlin.Boolean" -> json.getBoolean(parseParams.fieldName)
                type.startsWith("kotlin.collections.List") -> {
                    val arg = parseParams.parameter.type.arguments[0]
                    if (arg.type?.isSubtypeOf(Serializable::class.starProjectedType) == true) {
                        jsonArrayToList(json.getJSONArray(parseParams.fieldName), parser = { jsonStr ->
                            //parse<>(jsonStr)
                        })
                    } else {

                    }
                    println("it")
                    //defaultType()parseParams.parameter.type.arguments is Serializable)
                    val array = json.getJSONArray(parseParams.fieldName)

                }
                else -> {
                    println("Type: ${parseParams.parameter.type.toString()}")
                    json.get(parseParams.fieldName)
                }
            }
            parseParams.parameter to value

        }.onEach {
            println("${it.key.index}>>> ${it.value} -> ${it.key}")
        }
        val instance = primaryConstructor.callBy(args)
        instance
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()

    fun serialize(data: Any): JSONObject {
        return jsonObject {
            data.getSerialParams().forEach { field ->
                field.fieldName hasValue when (field.value) {
                    is Serializable -> serialize(field.value)
                    is String -> field.value
                    is Double -> field.value
                    is Int -> field.value
                    is Float -> field.value
                    is Boolean -> field.value
                    is Char -> field.value
                    is List<*> -> {
                        jsonArray {
                            val array = field.value.mapNotNull { value ->
                                when (value) {
                                    is Serializable -> serialize(value)
                                    is String -> value
                                    is Double -> value
                                    is Int -> value
                                    is Float -> value
                                    is Boolean -> value
                                    is Char -> value
                                    else -> null
                                }
                            }
                            putValues(array)
                        }
                    }
                    else -> {
                        logError(Tag, "Error while serializing: $data. Field: $field", NotImplementedError())
                    }
                }
            }
        }
    }

    private fun <T> jsonArrayToList(jsonArray: JSONArray, parser: (String) -> T): List<T> {
        return buildList {
            repeat(jsonArray.length()) { index ->
                add(parser(jsonArray.get(index).toString()))
            }
        }
    }

    private fun KClass<*>.getParseParams(): List<ParseParams> {
        val primaryConstructor = this.primaryConstructor!!
        val parameters = primaryConstructor.parameters
        val fields = this.declaredMemberProperties.mapNotNull { it.javaField }
        return parameters.map { parameter ->
            val field = fields.firstOrNull { it.name == parameter.name }
            require(field?.isAnnotationPresent(JsonFieldName::class.java) == true)
            ParseParams(
                fieldName = field!!.getAnnotation(JsonFieldName::class.java)!!.key,
                parameter = parameter
            )
        }
    }

    private fun Any.getSerialParams(): List<SerialParams> {
        return this::class.declaredMemberProperties.mapNotNull { field ->
            if (field.javaField?.isAnnotationPresent(JsonFieldName::class.java) == true) {
                val annotation = field.javaField?.getAnnotation(JsonFieldName::class.java)
                requireNotNull(annotation) {
                    "No annotation @SerialName set to field: ${field.name}"
                }
                SerialParams(
                    fieldName = annotation.key,
                    value = readInstanceProperty(this, field.name)
                )
            } else {
                null
            }
        }
    }

    private fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
        val property = instance::class.members
            // don't cast here to <Any, R>, it would succeed silently
            .first { it.name == propertyName } as KProperty1<Any, *>
        // force a invalid cast exception if incorrect type here
        return property.get(instance) as R
    }

    class SerialParams(
        val fieldName: String,
        val value: Any,
    )

    data class ParseParams(
        val fieldName: String,
        val parameter: KParameter
    )
}

//    inline fun <reified T : Serializable> serialize2(data: T): String {
//        val result = (T::class).declaredMemberProperties.mapNotNull { property ->
//            val value = property.getValue(data, property)
//            value
//
//        }
//        println("res = $result")
//        return jsonObject {
//
//        }.toString()
//    }
//
//    inline fun <reified T : Serializable> serialize(type: KClass<T>, data: T): String {
//        val result = type.memberProperties.mapNotNull { property ->
//            property.getValue(data, property)
//            property.javaField?.annotations?.filterIsInstance<SerialName>()?.firstOrNull()?.let {
//                it.key to property.getValue(data, property)
//            }
//        }.toMap()
//        return jsonObject {
//            result.forEach { (key, value) ->
//                key hasValue when (value) {
//                    is Serializable -> {
//                        val clazz2 = value::class
//                        //serialize(value)
//                    }
//                    is List<*> -> {
//
//                    }
//                    else -> value
//                }
//            }
//        }.toString()
//    }
//
//    fun <T : Any> parse(data: String?): T? {
//        TODO("Not yet implemented")
//    }

//    private fun Any.getFields(): Map<String, String> {
//        return this::class.declaredMemberProperties.mapNotNull { field ->
//            if (field.javaField?.isAnnotationPresent(SkipField::class.java) == true) {
//                null
//            } else {
//                val annotation = field.javaField?.getAnnotation(SerialName::class.java)
//                requireNotNull(annotation) {
//                    "No annotation @SerialName set to field: ${field.name}"
//                }
//                field.name to annotation.key
//            }
//        }.toMap()
//    }

private const val Tag = "BidonSerializer"