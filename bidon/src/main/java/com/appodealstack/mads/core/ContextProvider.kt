package com.appodealstack.mads.core

import android.content.Context
import com.appodealstack.mads.core.impl.ContextProviderImpl

internal val ContextProvider: IContextProvider by lazy { ContextProviderImpl() }

internal interface IContextProvider {
    val requiredContext: Context
    fun setContext(context: Context)
}

