package com.appodealstack.bidon.databinders.token

import com.appodealstack.bidon.config.models.Token
import com.appodealstack.bidon.utils.keyvaluestorage.KeyValueStorage

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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