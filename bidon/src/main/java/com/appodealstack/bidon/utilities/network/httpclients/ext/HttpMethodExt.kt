package com.appodealstack.bidon.utilities.network.httpclients.ext

import com.appodealstack.bidon.utilities.network.HttpClient

internal fun HttpClient.Method.asRequestMethod() = when (this) {
    HttpClient.Method.GET -> "GET"
    HttpClient.Method.POST -> "POST"
    HttpClient.Method.PUT -> "PUT"
    HttpClient.Method.DELETE -> "DELETE"
}
