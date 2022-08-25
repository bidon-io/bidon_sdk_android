package com.appodealstack.bidon.utilities.ktor

import com.appodealstack.bidon.BuildConfig
import com.appodealstack.bidon.core.ext.logInfo
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val BidonHttpClient by lazy {
    HttpClient(OkHttp) {
//        install(ContentEncoding) {
//            gzip()
//        }
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
        install(HttpRequestRetry) {
            var retryDelay: Long? = null
            retryIf { _, response ->
                (!response.status.isSuccess() && response.headers.contains(HttpHeaders.RetryAfter)).also {
                    retryDelay = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.let { headerRetryDelay ->
                        // if [Retry-After] is in seconds, it < 100, else it has to be milliseconds.
                        headerRetryDelay.takeIf { it > 100L } ?: headerRetryDelay.times(1000)
                    }
                    logInfo(Tag, "Request failed. Retry after $retryDelay ms.")
                }
            }
            delayMillis(respectRetryAfterHeader = false) { retry ->
                retryDelay ?: retry.toLong()
            }
        }
        defaultRequest {
            header("X-BidOn-Version", BidOnSdkVersion)
//            header("Accept-Encoding", "gzip")
//            header("Content-Encoding", "gzip")
        }
    }
}

private val BidOnSdkVersion by lazy { BuildConfig.ADAPTER_VERSION }
private const val Tag = "BidonHttpClient"
