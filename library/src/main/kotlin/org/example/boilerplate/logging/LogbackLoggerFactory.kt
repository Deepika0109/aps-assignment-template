package org.example.boilerplate.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class LogbackLoggerFactory : LoggerFactory {
    private val root = org.slf4j.LoggerFactory.getLogger("ROOT") as ch.qos.logback.classic.Logger
    private val cached = mutableMapOf<String, Logger>()

    init {
        root.level = Level.INFO

        root.detachAndStopAllAppenders()
        overrideDefaultColors()

        val patternEncoder = PatternLayoutEncoder()
        patternEncoder.pattern =
            "%date{HH:mm:ss.SSS} %highlightex(%5level) %yellow([%logger{10}]) %highlightex(%msg%n%throwable)"
        patternEncoder.context = root.loggerContext
        patternEncoder.start()

        val logConsoleAppender = ConsoleAppender<ILoggingEvent>()
        root.addAppender(logConsoleAppender)

        logConsoleAppender.context = root.loggerContext
        logConsoleAppender.name = "console"
        logConsoleAppender.encoder = patternEncoder
        logConsoleAppender.start()
    }

    override fun create(name: String): Logger {
        return cached.getOrPut(name) {
            val logger = root.loggerContext.getLogger(name)
            return@getOrPut object : Logger {
                override fun trace(exception: Throwable?, block: () -> String) {
                    if (logger.isTraceEnabled) logger.trace(block(), exception)
                }

                override fun debug(exception: Throwable?, block: () -> String) {
                    if (logger.isDebugEnabled) logger.debug(block(), exception)
                }

                override fun info(exception: Throwable?, block: () -> String) {
                    if (logger.isInfoEnabled) logger.info(block(), exception)
                }

                override fun warn(exception: Throwable?, block: () -> String) {
                    if (logger.isWarnEnabled) logger.warn(block(), exception)
                }

                override fun error(exception: Throwable?, block: () -> String) {
                    if (logger.isErrorEnabled) logger.error(block(), exception)
                }

                override fun slf4j(): org.slf4j.Logger {
                    return logger
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun overrideDefaultColors() {
        val context = root.loggerContext
        val registry = context.getObject(CoreConstants.PATTERN_RULE_REGISTRY) as? HashMap<String, String> ?: hashMapOf()

        registry["highlightex"] = OverrideColors::class.java.name
        root.loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry)
    }

    internal class OverrideColors : ForegroundCompositeConverterBase<ILoggingEvent>() {
        override fun getForegroundColorCode(event: ILoggingEvent): String {
            val level = event.level
            return when (level.toInt()) {
                Level.TRACE_INT -> ANSIConstants.CYAN_FG
                Level.DEBUG_INT -> ANSIConstants.MAGENTA_FG
                Level.INFO_INT -> ANSIConstants.GREEN_FG
                Level.WARN_INT -> ANSIConstants.YELLOW_FG
                Level.ERROR_INT -> ANSIConstants.BOLD + ANSIConstants.RED_FG
                else -> ANSIConstants.DEFAULT_FG
            }
        }
    }
}