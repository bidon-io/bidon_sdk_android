package com.appodealstack.bidon.di

import com.appodealstack.bidon.di.SimpleInjection.Scope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

/**
 * val instanceSomeClass = get<SomeClass>()
 */
internal inline fun <reified T : Any> get(): T = SimpleInjection.getInstance()

/**
 * val instanceSomeClass = get<SomeClass> {
 *    params(someParam1ToSomeClass, someParam2ToSomeClass)
 * }
 */
internal inline fun <reified T : Any> get(params: FactoryParams.() -> Unit): T = SimpleInjection.getInstance(params)

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

    private val factories = mutableMapOf<KClass<*>, Factory<*>>()
    private val paramFactories = mutableMapOf<KClass<*>, ParamFactory<*>>()
    private val singletons = mutableMapOf<KClass<*>, Singleton<*>>()

    inline fun <reified T : Any> addFactory(factory: Factory<T>) {
        factories[T::class] = factory
    }

    inline fun <reified T : Any> addFactoryWithParams(factory: ParamFactory<T>) {
        paramFactories[T::class] = factory
    }

    inline fun <reified T : Any> addSingleton(singleton: Factory<T>) {
        singletons[T::class] = Singleton(singleton)
    }

    inline fun <reified T : Any> getInstance(): T {
        val instance = (factories[T::class]?.invoke() as? T) ?: (singletons[T::class]?.instance as? T)
        return requireNotNull(instance) {
            "No factory provided for class: ${T::class.java}"
        }
    }

    inline fun <reified T : Any> getInstance(parameters: FactoryParams.() -> Unit): T {
        val factoryParams = FactoryParams().apply(parameters).getParameters()
        val instance = (paramFactories[T::class]?.invoke(*factoryParams) as? T)
        return requireNotNull(instance) {
            "No factory provided for class: ${T::class.java}"
        }
    }

    internal fun interface Factory<T> {
        operator fun invoke(): T
    }

    internal fun interface ParamFactory<T> {
        operator fun invoke(vararg params: Any): T
    }

    internal class Singleton<T>(private val factory: Factory<T>) {
        val instance: T by lazy {
            factory()
        }
    }

    internal class Scope {
        inline fun <reified T : Any> factory(factory: Factory<T>) {
            addFactory(factory)
        }

        inline fun <reified T : Any> factoryWithParams(factory: ParamFactory<T>) {
            addFactoryWithParams(factory)
        }

        inline fun <reified T : Any> singleton(singleton: Factory<T>) {
            addSingleton(singleton)
        }
    }
}
