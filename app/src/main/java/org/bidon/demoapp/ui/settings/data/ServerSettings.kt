package org.bidon.demoapp.ui.settings.data

import android.util.Base64
import org.bidon.demoapp.BuildConfig.STAGING_BASIC_AUTH_PASSWORD
import org.bidon.demoapp.BuildConfig.STAGING_BASIC_AUTH_USERNAME

internal data class ServerSettingsState(
    val hosts: List<Host>,
    val selected: Host
)

internal sealed interface Host {
    val name: String
    val baseUrl: String

    object Production : Host {
        override val baseUrl: String = "https://b.appbaqend.com"
        override val name: String = "Production"
    }

    object MockServer : Host {
        override val baseUrl: String = "https://ef5347ef-7389-4095-8a57-cc78c827f8b2.mock.pstmn.io"
        override val name: String = "Mock"
    }

    class Staging(val prefix: String) : Host {
        override val name: String = "Staging"
        override val baseUrl: String = "$SCHEME$prefix$SUFFIX"
        fun getBasicAuth(): String {
            val username = STAGING_BASIC_AUTH_USERNAME
            val password = STAGING_BASIC_AUTH_PASSWORD
            return Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.DEFAULT).also {
                println("Authorization: Basic $it")
            }
        }

        companion object {
            const val SCHEME = "https://"
            const val SUFFIX = "-app.bidon.org"
            val DEFAULT get() = Staging("staging1")
            fun fromString(string: String): Staging {
                return Staging(
                    string
                        .removePrefix(SCHEME)
                        .removeSuffix(SUFFIX)
                )
            }
        }
    }

    companion object {
        internal fun fromString(string: String?): Host {
            return when (string) {
                Production.baseUrl -> Production
                MockServer.baseUrl -> MockServer
                else -> string?.let { Staging.fromString(it) } ?: Production
            }
        }

        fun values(savedString: String?): ServerSettingsState {
            val saved = fromString(savedString)
            return ServerSettingsState(
                hosts = buildList {
                    add(Production)
                    add(MockServer)
                    val staging = if (saved is Staging) {
                        saved
                    } else {
                        Staging.DEFAULT
                    }
                    add(staging)
                },
                selected = saved
            )
        }
    }
}