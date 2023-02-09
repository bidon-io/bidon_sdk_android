package com.appodealstack.bidon.data.networking.impl

import com.appodealstack.bidon.data.networking.Method

internal fun Method.asRequestMethod() = when (this) {
    Method.GET -> "GET"
    Method.POST -> "POST"
    Method.PUT -> "PUT"
    Method.DELETE -> "DELETE"
}