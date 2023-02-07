package com.appodealstack.bidon.domain.adapter

import android.app.Activity
import kotlinx.serialization.json.JsonObject

/**
 * Created by Aleksei Cherniaev on 07/09/2023.
 */
interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
    fun parseConfigParam(json: JsonObject): T
}