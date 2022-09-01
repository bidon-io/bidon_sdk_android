package com.appodealstack.bidon.utilities.datasource.token

import com.appodealstack.bidon.config.data.models.Token
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage

internal class TokenDataSourceImpl(
    private val keyValueStorage: KeyValueStorage
) : TokenDataSource {

    /**
     * @return current server token which was provided by new "init" request
     * (what is actual different from [KeyValueStorage.token] ()})
     */
    private var token: String? = null
        get() = field ?: keyValueStorage.token

    override fun getCachedToken(): Token? {
        return if (token == null) {
            null
        } else {
            token?.let { Token(token = it) }
        }
    }
}