package com.appodealstack.bidon.core

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.core.impl.ContextProviderImpl

internal interface ContextProvider {
    val activity: Activity?
    val requiredContext: Context
    fun setContext(context: Context)
}

