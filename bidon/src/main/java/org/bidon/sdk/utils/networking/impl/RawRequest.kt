package org.bidon.sdk.utils.networking.impl

import org.bidon.sdk.utils.networking.Method

internal class RawRequest(
    val method: Method,
    val url: String,
    val body: ByteArray?,
    val headers: Map<String, List<String>>,
)