package com.appodealstack.bidon.config.domain

/**
 * Mark field to be taken to JSON.
 *
data class TestMePack(
 @JsonFieldName("field_name_1")
 val fieldString: String,
 @JsonFieldName("field_name_2")
 val fieldInt: Int,
 @SkipField
 val fieldShouldNotBeTaken: String,
)
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class JsonFieldName(
    val value: String
)

/**
 * Mark field to be skipped in JSON
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SkipField
