package com.appodealstack.bidon.core

import android.app.Activity
import android.content.Context

internal interface ContextProvider {
    val activity: Activity?
    val requiredContext: Context
    fun setContext(context: Context)
}

