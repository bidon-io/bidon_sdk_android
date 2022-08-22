package com.appodealstack.bidon.config.domain.ext

import com.appodealstack.bidon.config.domain.JsonFieldName
import com.appodealstack.bidon.config.domain.SkipField
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

fun Any.getJsonFieldNamesWithValues(): Map<String, Any?> {
    return this::class.declaredMemberProperties.mapNotNull { field ->
        if (field.javaField?.isAnnotationPresent(SkipField::class.java) == true) {
            null
        } else {
            val annotation = field.javaField?.getAnnotation(JsonFieldName::class.java)
            val value = field.getter.call(this)
            requireNotNull(annotation) {
                "No annotation @JsonFieldName set to field: ${field.name}"
            }
            annotation.value to value
        }
    }.toMap()
}
