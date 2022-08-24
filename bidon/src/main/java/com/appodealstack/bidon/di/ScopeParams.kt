package com.appodealstack.bidon.di

internal class ScopeParams {
    private var parameters: Any? = null

    fun getParameters() = requireNotNull(parameters)

    fun params(parameters: Any) {
        this.parameters = parameters
    }
}