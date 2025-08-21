package org.example.primary.validation

import org.example.boilerplate.errors.ValidationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/** Runtime registry for request validators keyed by payload type. */
object ValidationRegistry {
    private val map = ConcurrentHashMap<KClass<*>, Validator<*>>() // thread-safe

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): Validator<T>? = map[type] as? Validator<T>

    /** Replace any existing validator for this type. */
    fun <T : Any> register(type: KClass<T>, validator: Validator<T>) {
        map[type] = validator
    }

    /** Append a validator (auto-wrap into CompositeValidator as needed). */
    fun <T : Any> add(type: KClass<T>, validator: Validator<T>) {
        val existing = map[type]
        map[type] = when (existing) {
            null -> validator
            is CompositeValidator<*> -> {
                @Suppress("UNCHECKED_CAST")
                val current = existing as CompositeValidator<T>
                CompositeValidator(*arrayOf(*current.validators, validator))
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                CompositeValidator(existing as Validator<T>, validator)
            }
        }
    }

    /** Replace all validators in one go. */
    fun <T : Any> set(type: KClass<T>, vararg validators: Validator<T>) {
        map[type] = CompositeValidator(*validators)
    }

    /** For unit tests to avoid cross-test leakage. */
    fun clear() = map.clear()
}

/** Extension: run all registered validators for T, aggregate errors, then return T if OK. */
inline fun <reified T : Any> T.validateWithRegistry(): T {
    val v = ValidationRegistry.get(T::class)
    val errs = v?.validate(this) ?: emptyList()
    if (errs.isNotEmpty()) throw ValidationException(errs)
    return this
}
