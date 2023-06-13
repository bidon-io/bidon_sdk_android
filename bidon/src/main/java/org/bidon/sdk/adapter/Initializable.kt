package org.bidon.sdk.adapter

import android.app.Activity

/**
 * Created by Bidon Team on 07/09/2023.
 */
interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
    fun parseConfigParam(json: String): T
}