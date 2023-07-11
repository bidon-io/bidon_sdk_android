package org.bidon.sdk.adapter

import android.content.Context

/**
 * Created by Bidon Team on 07/09/2023.
 */
interface Initializable<T : AdapterParameters> {
    suspend fun init(context: Context, configParams: T)
    fun parseConfigParam(json: String): T
}