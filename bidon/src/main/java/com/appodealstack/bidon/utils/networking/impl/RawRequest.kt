package com.appodealstack.bidon.utils.networking.impl

import com.appodealstack.bidon.utils.networking.Method

internal class RawRequest(
    val method: Method,
    val url: String,
    val body: ByteArray?,
    val headers: Map<String, List<String>>,
)