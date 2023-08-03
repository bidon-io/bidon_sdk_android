package org.bidon.sdk.utils.serializer

import kotlin.reflect.full.*

/**
 * Created by Bidon Team on 14/02/2023.
 */

internal interface Serializable {
    sealed class Error : Throwable() {
        object UnknownClass : Error()
        object NotAnnotatedField : Error()
    }
}

@Target(AnnotationTarget.FIELD)
internal annotation class JsonName(val key: String)

internal fun Serializable.serialize() = BidonSerializer.serialize(this)
internal fun List<Serializable>.serialize() = BidonSerializer.serializeToArray(this)
