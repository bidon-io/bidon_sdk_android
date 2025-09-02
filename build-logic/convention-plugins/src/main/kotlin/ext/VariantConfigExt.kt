package ext

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.Variant

private fun Variant.addBuildConfigField(
    type: BuildConfigFieldType,
    name: String,
    value: Any
) {
    when (type) {
        BuildConfigFieldType.String -> {
            require(value is String)
            buildConfigFields.put(
                name,
                BuildConfigField(
                    type = "String",
                    comment = name,
                    value = "\"$value\""
                ),
            )
        }

        BuildConfigFieldType.Integer -> {
            require(value is Int)
            buildConfigFields.put(
                name,
                BuildConfigField(
                    type = "int",
                    comment = name,
                    value = "\"$value\""
                ),
            )
        }

        BuildConfigFieldType.Date -> {
            require(value is Long)
            buildConfigFields.put(
                name,
                BuildConfigField(
                    type = "java.util.Date",
                    comment = name,
                    value = "\"$value\""
                ),
            )
        }
    }
}

/**
 * Bidon API Key property only Test Application module
 */
var Variant.BIDON_API_KEY: String
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
var Variant.STAGING_BASIC_AUTH_USERNAME: String?
    get() = error("Property STAGING_BASIC_AUTH_USERNAME couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "STAGING_BASIC_AUTH_USERNAME",
            value = value ?: return
        )
    }

var Variant.STAGING_BASIC_AUTH_PASSWORD: String?
    get() = error("Property STAGING_BASIC_AUTH_PASSWORD couldn't be get")
    set(value) {
        addBuildConfigField(
            type = BuildConfigFieldType.String,
            name = "STAGING_BASIC_AUTH_PASSWORD",
            value = value ?: return
        )
    }