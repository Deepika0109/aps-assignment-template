package org.example.boilerplate.logging

import org.slf4j.Logger

interface Logger {
    fun trace(exception: Throwable? = null, block: () -> String)
    fun debug(exception: Throwable? = null, block: () -> String)
    fun info(exception: Throwable? = null, block: () -> String)
    fun warn(exception: Throwable? = null, block: () -> String)
    fun error(exception: Throwable? = null, block: () -> String)
    fun slf4j(): Logger
}