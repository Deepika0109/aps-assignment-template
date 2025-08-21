package org.example.primary

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.example.boilerplate.Server
import org.example.boilerplate.errors.ApiError
import org.example.boilerplate.errors.FieldError
import org.example.primary.outbox.OutboxPublisher
import org.example.primary.task.*
import org.example.primary.validation.CompositeValidator
import org.example.primary.validation.ValidationRegistry
import org.example.primary.validation.validateWithRegistry
import java.util.*

/**
 * Automatically create and run a Ktor Server that is set up to:
 * * Listen on port 9055 for incoming requests;
 * * Deserialize and serialize JSON from requests or to responses;
 */
fun main() = Server.run(port = 9055) { entrypoint() }

/**
 * This function is automatically called at the right time when invoking [Server.run].
 * You can consider this the entrypoint of the application.
 */
fun Application.entrypoint() {
    DatabaseFactory.init(environment)

    // Register validators once
    ValidationRegistry.register(
        CreateTask::class,
        CompositeValidator(NameRequiredNonBlank, DueDateIsoIfPresentOnCreate)
    )
    ValidationRegistry.register(
        UpdateTask::class,
        CompositeValidator(UpdateNameNonBlankIfPresent, DueDateIsoIfPresentOnUpdate, AssigneePatchStructureIfPresent)
    )

    OutboxPublisher.start()
    environment.monitor.subscribe(ApplicationStopped) { OutboxPublisher.stop() }

    val service = TaskService()
    installTaskRoutes(service)
    log.info("primary-app ready on :9055")
}

fun Application.installTaskRoutes(service: TaskService) {
    routing {
        get("/health") { call.respondText("OK") }

        route("/api/v2/task") {
            get("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_error", "Invalid request payload.", listOf(FieldError("id","invalid_uuid")))
                    )
                val task = service.get(id)
                call.respond(HttpStatusCode.OK, task.toResponse())
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_error", "Invalid request payload.", listOf(FieldError("id","invalid_uuid")))
                    )
                service.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
            post {
                val req = call.receive<CreateTask>().validateWithRegistry()
                val created = service.create(req)
                call.respond(HttpStatusCode.OK, created.toResponse())
            }
            put("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_error", "Invalid request payload.", listOf(FieldError("id","invalid_uuid")))
                    )
                val req = call.receive<UpdateTask>().validateWithRegistry()
                val updated = service.update(id, req)
                call.respond(HttpStatusCode.OK, updated.toResponse())
            }
        }
    }
}