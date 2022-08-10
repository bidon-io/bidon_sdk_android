package com.appodealstack.bidon.core.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.core.ContextProvider

internal class ContextProviderImpl : ContextProvider {
    private var context: Context? = null

    override val activity: Activity?
        get() = context as? Activity

    override val requiredContext: Context
        get() = requireNotNull(context)

    override fun setContext(context: Context) {
        this.context = context
    }
}