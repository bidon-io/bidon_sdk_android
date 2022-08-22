package com.appodeal.mads.ui.settings.data

internal data class ServerSettings(
    val host: Host,
    val port: Int?
)

internal enum class Host {
    Production,
    MockServer
}

internal val Ports = listOf(
    null,
    443,
    80,
    3000,
    8080,
    8081,
)
