package org.example.boilerplate

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.example.boilerplate.errors.ApiError
import org.example.boilerplate.errors.ValidationException
import org.example.boilerplate.logging.LoggerFactory
import org.example.boilerplate.logging.create
import java.util.concurrent.atomic.AtomicBoolean

object Server {
    private val started = AtomicBoolean(false)
    /**
     * Create and start a Ktor Server with the boilerplate also done to get started quickly on the implementation
     */
    fun run(port: Int, func: Application.() -> Unit) {
        if (!started.compareAndSet(false, true)) {
            LoggerFactory.create<Server>().slf4j().warn("Server.run called again; already started. Ignoring.")
            return
        }
        // Create a custom environment in which we can assign our own logger implementation.
        val environment = applicationEnvironment {
            log = LoggerFactory.create<Server>().slf4j()
        }

        embeddedServer(
            factory = CIO,
            rootConfig = serverConfig(environment) {
                // We have it disabled by default since it's not really working that well, but you can give it a try if you'd like.
                // With development mode enabled, you have to run './gradlew -t build -x test -i' in a terminal as well
                // This will recompile the project when changes have been detected that can be picked up by Ktor.
                developmentMode = false

                module {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(JSON.mapper))
                    }

                    install(StatusPages) {
                        exception<ValidationException> { call, ex ->
                            call.respond(ex.status, ex.body)
                        }
                        exception<Throwable> { call, ex ->
                            call.application.environment.log.error("Unhandled error", ex)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError("internal_error", "Something went wrong. Please try again.")
                            )
                        }
                    }

                    // Automatically call the entrypoint for the assignment
                    func()
                }
            },
            configure = {
                connector {
                    this.port = port
                }
            }
        ).start(wait = true)
    }
}