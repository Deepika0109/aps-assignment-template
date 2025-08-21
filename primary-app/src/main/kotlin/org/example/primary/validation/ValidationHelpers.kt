package org.example.primary.validation

import org.example.boilerplate.errors.FieldError


/** Contract for any payload validator. */
fun interface Validator<T> {
    fun validate(value: T): List<FieldError>
}

/** Compose many validators into one. */
class CompositeValidator<T>(vararg val validators: Validator<T>) : Validator<T> {
    override fun validate(value: T): List<FieldError> =
        validators.flatMap { it.validate(value) }
}

