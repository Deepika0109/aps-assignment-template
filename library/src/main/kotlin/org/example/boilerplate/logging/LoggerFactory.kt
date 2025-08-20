package org.example.boilerplate.logging

import kotlin.reflect.KClass

interface LoggerFactory {
    fun create(name: String): Logger

    companion object {
        private val instance = LogbackLoggerFactory()

        /**
         * Create a new logger using the provided name as the logger name.
         */
        fun create(name: String): Logger {
            return instance.create(name)
        }

        /**
         * Create a new logger based on a class type using the qualified name as the logger name.
         */
        fun create(type: KClass<*>): Logger {
            return instance.create(type.qualifiedName!!)
        }
    }
}