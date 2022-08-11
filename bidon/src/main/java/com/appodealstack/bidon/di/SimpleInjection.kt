package com.appodealstack.bidon.di
import com.appodealstack.bidon.di.SimpleInjection.Scope

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
internal fun registerDependencyInjection(module: Scope.() -> Unit) {
    Scope().apply(module)
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

    internal class Scope {
        inline fun <reified T : Any> factory(noinline factory: () -> T) {
            addFactory(factory)
        }

        inline fun <reified T : Any> singleton(noinline singleton: () -> T) {
            addSingleton(singleton)
        }
    }
}

