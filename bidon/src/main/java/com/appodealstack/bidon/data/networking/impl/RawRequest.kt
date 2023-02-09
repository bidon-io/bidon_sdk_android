package com.appodealstack.bidon.data.networking.impl

import com.appodealstack.bidon.data.networking.Method

internal class RawRequest(
    val method: Method,
    val url: String,
    val body: ByteArray?,
    val headers: Map<String, List<String>>,
)