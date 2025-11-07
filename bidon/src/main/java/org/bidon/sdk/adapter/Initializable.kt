package org.bidon.sdk.adapter

import android.content.Context

/**
 * Created by Bidon Team on 07/09/2023.
 */
public interface Initializable<T : AdapterParameters> {
    public suspend fun init(context: Context, configParams: T)
    public fun parseConfigParam(json: String): T
}