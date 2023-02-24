package org.bidon.sdk.databinders.token

import org.bidon.sdk.config.models.Token
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage

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
    override var token: Token? = null
        get() {
            println("TOKEN GET $field")
            return field ?: keyValueStorage.token?.let { Token(it) }?.also {
                field = it
            }
        }
        set(value) {
            println("TOKEN SET $field -> $value")
            keyValueStorage.token = value?.token
            field = value
        }
}