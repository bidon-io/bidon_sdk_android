package com.appodealstack.bidon.utilities.ktor

import com.appodealstack.bidon.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val BidonHttpClient by lazy {
    HttpClient(OkHttp) {
        install(ContentEncoding) {
             gzip()
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
        defaultRequest {
            // TODO add support Retry-After header field (milliseconds).
            header("X-BidOn-Version", BidOnSdkVersion)
        }
    }
}

private val BidOnSdkVersion by lazy { BuildConfig.ADAPTER_VERSION }