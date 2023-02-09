package com.appodealstack.bidon.data.json

import com.appodealstack.bidon.data.models.auction.*
import com.appodealstack.bidon.data.models.config.*
import com.appodealstack.bidon.data.models.stats.DemandSerializer
import com.appodealstack.bidon.data.models.stats.ImpressionRequestBodySerializer
import com.appodealstack.bidon.data.models.stats.RoundSerializer
import com.appodealstack.bidon.data.models.stats.StatsRequestBodySerializer
import com.appodealstack.bidon.data.networking.BaseResponseErrorParser
import com.appodealstack.bidon.data.networking.BaseResponseParser
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
@Suppress("RemoveRedundantQualifierName", "UNCHECKED_CAST")
internal object JsonParsers {

    private val parsersFactories = mutableMapOf<KClass<*>, ParserFactory<*>>()
    private val serializersFactories = mutableMapOf<KClass<*>, SerializerFactory<*>>()

    init {
        addSerializer { AdObjectRequestBodySerializer() }
        addSerializer { BannerRequestBodySerializer() }
        addSerializer { InterstitialRequestBodySerializer() }
        addSerializer { RewardedRequestBodySerializer() }
        addSerializer { AdapterInfoSerializer() }
        addSerializer { SessionSerializer() }
        addSerializer { UserSerializer() }
        addSerializer { GeoSerializer() }
        addSerializer { PlacementSerializer() }
        addSerializer { RewardSerializer() }
        addSerializer { CappingSerializer() }
        addSerializer { DeviceSerializer() }
        addSerializer { ImpressionRequestBodySerializer() }
        addSerializer { StatsRequestBodySerializer() }
        addSerializer { RoundSerializer() }
        addSerializer { DemandSerializer() }
        addSerializer { AppSerializer() }

        addParser { BaseResponseParser() }
        addParser { BaseResponseErrorParser() }
        addParser { ConfigResponseParser() }
        addParser { AuctionResponseParser() }
        addParser { RoundParser() }
        addParser { LineItemParser() }
    }

    inline fun <reified T : Any> serializeOrNull(data: T?): JSONObject? {
        return if (data == null) {
            null
        } else {
            (serializersFactories[T::class] as SerializerFactory<T>).instance.serialize(data)
        }
    }

    inline fun <reified T : Any> serialize(data: T): JSONObject {
        return (serializersFactories[T::class] as SerializerFactory<T>).instance.serialize(data)
    }

    inline fun <reified T : Any> getSerializer(): JsonSerializer<T> {
        return (serializersFactories[T::class] as SerializerFactory<T>).instance
    }

    inline fun <reified T : Any> parseOrNull(jsonString: String): T? {
        return (parsersFactories[T::class] as ParserFactory<T>).instance.parseOrNull(jsonString)
    }

    inline fun <reified T : Any> parseList(jsonArray: JSONArray?): List<T>? {
        if (jsonArray == null) return null
        val parser = (parsersFactories[T::class] as ParserFactory<T>).instance
        return buildList {
            repeat(jsonArray.length()) { index ->
                parser.parseOrNull(jsonArray.getJSONObject(index).toString())?.let {
                    add(it)
                }
            }
        }
    }

    private inline fun <reified T : Any> addSerializer(noinline serializer: () -> JsonSerializer<T>): JsonParsers {
        serializersFactories[T::class] = SerializerFactory(factory = serializer)
        return this
    }

    private inline fun <reified T : Any> addParser(noinline parser: () -> JsonParser<T>): JsonParsers {
        parsersFactories[T::class] = ParserFactory(factory = parser)
        return this
    }

    internal class ParserFactory<T>(private val factory: () -> JsonParser<T>) {
        val instance: JsonParser<T> get() = factory.invoke()
    }

    internal class SerializerFactory<T>(private val factory: () -> JsonSerializer<T>) {
        val instance: JsonSerializer<T> get() = factory.invoke()
    }
}
