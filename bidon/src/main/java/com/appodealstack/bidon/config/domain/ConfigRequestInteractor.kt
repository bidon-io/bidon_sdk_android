package com.appodealstack.bidon.config.domain

internal interface ConfigRequestInteractor {
    suspend fun request(body: ConfigRequestBody): Result<ConfigResponse>
}

data class ConfigResponse(
    val initializationTimeout: Long,
    val adapters: List<AdapterInitializationInfo>
)

interface AdapterInitializationInfo {
    val id: String
}

data class ConfigRequestBody(
    val adapters: List<AdapterInfo>
)

data class AdapterInfo(
    @JsonFieldName("id")
    val id: String,
    @JsonFieldName("version")
    val adapterVersion: String,
    @JsonFieldName("sdk_version")
    val bidonSdkVersion: String
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class JsonFieldName(
    val value: String
)