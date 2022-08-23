package com.appodealstack.bidon.di

internal class ScopeParams {
    var parameters: Any? = null

    fun params(parameters: Any) {
        this.parameters = parameters
    }
}