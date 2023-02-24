package org.bidon.sdk.utils.serializer

import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

// TODO in progress
internal object BidonParser {
    data class ParseParams(
        val fieldName: String,
        val parameter: KParameter
    )

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
                            // parse<>(jsonStr)
                        })
                    } else {
                    }
                    println("it")
                    // defaultType()parseParams.parameter.type.arguments is Serializable)
                    val array = json.getJSONArray(parseParams.fieldName)
                }
                else -> {
                    println("Type: ${parseParams.parameter.type}")
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

    private fun KClass<*>.getParseParams(): List<ParseParams> {
        val primaryConstructor = this.primaryConstructor!!
        val parameters = primaryConstructor.parameters
        val fields = this.declaredMemberProperties.mapNotNull { it.javaField }
        return parameters.map { parameter ->
            val field = fields.firstOrNull { it.name == parameter.name }
            require(field?.isAnnotationPresent(JsonName::class.java) == true)
            ParseParams(
                fieldName = field!!.getAnnotation(JsonName::class.java)!!.key,
                parameter = parameter
            )
        }
    }

    private fun <T> jsonArrayToList(jsonArray: JSONArray, parser: (String) -> T): List<T> {
        return buildList {
            repeat(jsonArray.length()) { index ->
                add(parser(jsonArray.get(index).toString()))
            }
        }
    }
}