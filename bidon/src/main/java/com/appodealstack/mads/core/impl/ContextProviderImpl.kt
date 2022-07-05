package com.appodealstack.mads.core.impl

import android.content.Context
import com.appodealstack.mads.core.IContextProvider

internal class ContextProviderImpl : IContextProvider {
    private var context: Context? = null

    override val requiredContext: Context
        get() = requireNotNull(context)

    override fun setContext(context: Context) {
        this.context = context
    }
}