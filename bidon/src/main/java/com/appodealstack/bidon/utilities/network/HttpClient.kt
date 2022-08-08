package com.appodealstack.bidon.utilities.network

import com.appodealstack.bidon.utilities.network.httpclients.jsonHttpClient
import com.appodealstack.bidon.utilities.network.httpclients.protoHttpClient
import com.appodealstack.bidon.utilities.network.httpclients.zipBase64HttpClient
import com.appodealstack.bidon.utilities.network.httpclients.zipHttpClient

sealed interface HttpClient : Networking {
    /**
     * Use for uncompressed request body with Json
     */
    object Json : HttpClient, Networking by jsonHttpClient

    /**
     * Use for common Appodeal Consent Manager request: /check.
     */
    object Zip : HttpClient, Networking by zipHttpClient

    /**
     * Use for common Appodeal SDK requests: /show, /init, /get, etc.
     */
    object ZipBase64 : HttpClient, Networking by zipBase64HttpClient

    /**
     * Use for request body with Protobuf data: /stats
     */
    object Proto : HttpClient, Networking by protoHttpClient

    enum class Method {
        GET, POST, PUT, DELETE
    }
}