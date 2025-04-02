package org.bidon.sdk.utils.json

import org.bidon.sdk.auction.models.AdUnitParser
import org.bidon.sdk.auction.models.AuctionResponseParser
import org.bidon.sdk.config.models.ConfigResponseParser
import org.bidon.sdk.utils.networking.BaseResponseErrorParser
import org.bidon.sdk.utils.networking.BaseResponseParser
import org.json.JSONArray
import kotlin.reflect.KClass

/**
 * Created by Bidon Team on 08/02/2023.
 */
internal object JsonParsers {

    private val parsersFactories = mutableMapOf<KClass<*>, ParserFactory<*>>()

    init {
        addParser { BaseResponseParser() }
        addParser { BaseResponseErrorParser() }
        addParser { ConfigResponseParser() }
        addParser { AuctionResponseParser() }
        addParser { AdUnitParser() }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> parseOrNull(jsonString: String): T? {
        return (parsersFactories[T::class] as ParserFactory<T>).instance.parseOrNull(jsonString)
    }

    @Suppress("UNCHECKED_CAST")
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

    private inline fun <reified T : Any> addParser(noinline parser: () -> JsonParser<T>): JsonParsers {
        parsersFactories[T::class] = ParserFactory(factory = parser)
        return this
    }

    internal class ParserFactory<T>(private val factory: () -> JsonParser<T>) {
        val instance: JsonParser<T> get() = factory.invoke()
    }
}
