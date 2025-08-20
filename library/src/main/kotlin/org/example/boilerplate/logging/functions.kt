package org.example.boilerplate.logging

/**
 * Utility function to create a new logger for a class using the generic syntax.
 */
inline fun <reified T : Any> LoggerFactory.Companion.create(): Logger = create(T::class)