package com.appodealstack.bidon.di

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

/**
 * val instanceSomeClass = get<SomeClass>()
 */
internal inline fun <reified T : Any> get(): T = SimpleInjection.getInstance()

/**
 * val instanceSomeClass by inject<SomeClass>()
 */
internal inline fun <reified T : Any> inject(): ReadOnlyProperty<Nothing?, T> =
    ReadOnlyProperty { _, _ ->
        SimpleInjection.getInstance()
    }

/**
 * simpleInjectionModule {
 *     factory<C> { CImpl(a = get()) } // for register Factory.
 *     single<A> { AImpl() } // for register Singleton
 * }
 */
internal fun registerDependencyInjection(module: SimpleInjectionScope.() -> Unit) {
    SimpleInjectionScope().apply(module)
}

internal object SimpleInjection {

    private val factories = mutableMapOf<KClass<*>, Factory>()
    private val singletons = mutableMapOf<KClass<*>, Singleton>()

    inline fun <reified T : Any> addFactory(noinline factory: () -> T) {
        factories[T::class] = Factory(factory)
    }

    inline fun <reified T : Any> addSingleton(noinline factory: () -> T) {
        singletons[T::class] = Singleton(factory)
    }

    inline fun <reified T : Any> getInstance(): T {
        val instance = (factories[T::class]?.factory?.invoke() as? T)
            ?: (singletons[T::class]?.instance as? T)
        return requireNotNull(instance) {
            "No instance Singleton/Factory provided for class: ${T::class.java}"
        }
    }

    internal class Factory(val factory: () -> Any)
    internal class Singleton(private val factory: () -> Any) {
        val instance: Any by lazy {
            factory()
        }
    }
}

internal class SimpleInjectionScope {
    inline fun <reified T : Any> factory(noinline factory: () -> T) {
        SimpleInjection.addFactory(factory)
    }

    inline fun <reified T : Any> single(noinline singleton: () -> T) {
        SimpleInjection.addSingleton(singleton)
    }
}