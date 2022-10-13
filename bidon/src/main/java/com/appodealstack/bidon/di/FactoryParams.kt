package com.appodealstack.bidon.di

internal class FactoryParams {
    private var parameters: Array<out Any> = arrayOf()

    fun getParameters() = parameters

    fun params(vararg parameters: Any) {
        this.parameters = parameters
    }
}