package com.appodealstack.mads.base

import android.content.Context

internal val ContextProvider: IContextProvider by lazy { ContextProviderImpl() }

internal interface IContextProvider {
    val requiredContext: Context
    fun setContext(context: Context)
}

private class ContextProviderImpl : IContextProvider {
    private var context: Context? = null

    override val requiredContext: Context
        get() = requireNotNull(context)

    override fun setContext(context: Context) {
        this.context = context
    }
}
