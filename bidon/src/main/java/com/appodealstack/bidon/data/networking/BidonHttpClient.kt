package com.appodealstack.bidon.data.networking

import com.appodealstack.bidon.BuildConfig
import com.appodealstack.bidon.domain.stats.impl.logInfo
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal val BidonHttpClient by lazy {
    HttpClient(OkHttp) {
        install(ContentEncoding) {
            gzip()
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            register(ContentType.Application.Json, CustomJsonConverter())
            register(ContentType.Application.Xml, CustomXmlConverter())
        }
        install(HttpRequestRetry) {
            var retryDelay: Long? = null
            retryIf { _, response ->
                val needRetry = !response.status.isSuccess() && response.headers.contains(HttpHeaders.RetryAfter)
                if (needRetry) {
                    retryDelay = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.let { headerRetryDelay ->
                        // if [Retry-After] is in seconds, it < 100, else it has to be milliseconds.
                        headerRetryDelay.takeIf { it > 100L } ?: headerRetryDelay.times(1000)
                    }
                    logInfo(Tag, "Request failed. Retry after $retryDelay ms.")
                }
                needRetry
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
