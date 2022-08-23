package com.appodealstack.bidon.utilities.datasource.token

import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl

class TokenDataSourceImpl : TokenDataSource {

    /**
     * @return current server token which was provided by new "init" request
     * (what is actual different from [KeyValueStorageImpl.token] ()})
     */
    private var token: String? = null
        get() = field ?: KeyValueStorageImpl().token

    override fun getCachedToken(): String? {
        return token
    }
}