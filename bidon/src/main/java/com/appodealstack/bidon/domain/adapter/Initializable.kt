package com.appodealstack.bidon.domain.adapter

import android.app.Activity
import kotlinx.serialization.json.JsonObject

interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
    fun parseConfigParam(json: JsonObject): T
}