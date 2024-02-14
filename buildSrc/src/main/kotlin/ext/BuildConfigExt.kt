package ext

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.LibraryDefaultConfig
import com.android.build.api.dsl.VariantDimension

enum class BuildConfigFieldType {
    String,
    Integer,
    Date,
}

private fun VariantDimension.addBuildConfigField(
    type: BuildConfigFieldType,
    name: String,
    value: Any
) {
    when (type) {
        BuildConfigFieldType.String -> {
            require(value is String)
            buildConfigField(
                type = "String",
                name = name,
                value = "\"$value\""
            )
        }
        BuildConfigFieldType.Integer -> {
            require(value is Int)
            buildConfigField(
                type = "int",
                name = name,
                value = value.toString()
            )
        }
        BuildConfigFieldType.Date -> {
            require(value is Long)
            buildConfigField(
                type = "java.util.Date",
                name = name,
                value = "new java.util.Date(${value}L)"
            )
        }
    }
}

/**
 * Version property only for services and networks adapters
 */
var LibraryDefaultConfig.ADAPTER_VERSION: String
    get() = error("Property versionName couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "ADAPTER_VERSION",
            value = value
        )
    }

/**
 * SDK version property only for services and networks adapters
 */
var LibraryDefaultConfig.ADAPTER_SDK_VERSION: String
    get() = error("Property versionName couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "ADAPTER_SDK_VERSION",
            value = value
        )
    }

/**
 * Bidon API Key property only Test Application module
 */
var ApplicationDefaultConfig.BIDON_API_KEY: String
    get() = error("Property APP_KEY couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "BIDON_API_KEY",
            value = value
        )
    }

/**
 * Bidon Basic Auth property only for staging
 */
var ApplicationDefaultConfig.STAGING_BASIC_AUTH_USERNAME: String?
    get() = error("Property STAGING_BASIC_AUTH_USERNAME couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "STAGING_BASIC_AUTH_USERNAME",
            value = value ?: return
        )
    }

var ApplicationDefaultConfig.STAGING_BASIC_AUTH_PASSWORD: String?
    get() = error("Property STAGING_BASIC_AUTH_PASSWORD couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "STAGING_BASIC_AUTH_PASSWORD",
            value = value ?: return
        )
    }