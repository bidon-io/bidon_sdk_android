package com.appodealstack.bidon.utils.networking.impl

import com.appodealstack.bidon.utils.networking.Method

internal fun Method.asRequestMethod() = when (this) {
    Method.GET -> "GET"
    Method.POST -> "POST"
    Method.PUT -> "PUT"
    Method.DELETE -> "DELETE"
}