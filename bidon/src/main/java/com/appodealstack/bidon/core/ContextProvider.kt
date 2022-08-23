package com.appodealstack.bidon.core

import android.app.Activity
import android.content.Context

interface ContextProvider {
    val activity: Activity?
    val requiredContext: Context
    fun setContext(context: Context)
}

