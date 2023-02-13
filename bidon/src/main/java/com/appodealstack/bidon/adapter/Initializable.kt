package com.appodealstack.bidon.adapter

import android.app.Activity

/**
 * Created by Aleksei Cherniaev on 07/09/2023.
 */
interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
    fun parseConfigParam(json: String): T
}