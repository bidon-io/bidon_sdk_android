package org.bidon.sdk.utils.networking.impl

import org.bidon.sdk.utils.networking.Method

internal fun Method.asRequestMethod() = when (this) {
    Method.GET -> "GET"
    Method.POST -> "POST"
    Method.PUT -> "PUT"
    Method.DELETE -> "DELETE"
}