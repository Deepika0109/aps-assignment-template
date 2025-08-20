package org.example.primary

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import org.example.boilerplate.Server
import org.example.primary.outbox.OutboxPublisher
import org.example.primary.task.*
import java.util.*


/**
 * Automatically create and run a Ktor Server that is set up to:
 * * Listen on port 8080 for incoming requests;
 * * Deserialize and serialize JSON from requests or to responses;
 */
fun main() = Server.run(port = 9055) { entrypoint() }

/**
 * This function is automatically called at the right time when invoking [Server.run].
 * You can consider this the entrypoint of the application.
 */
fun Application.entrypoint() {
    DatabaseFactory.init(environment)
    OutboxPublisher.start()

    environment.monitor.subscribe(ApplicationStopped) {
        OutboxPublisher.stop()
        }
    val service = TaskService()
    // Routes as per the assignment
    installTaskRoutes(service)
    log.info("primary-app ready on :9055")
}

fun Application.installTaskRoutes(service: TaskService) {
    routing {
        get("/health") { call.respondText("OK") }

        route("/api/v2/task") {
            get("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                val task = service.get(id)
                call.respond(HttpStatusCode.OK, task.toResponse())
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                service.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
            post {
                val req = call.receive<CreateTask>().validate()
                val created = service.create(req)
                call.respond(HttpStatusCode.OK, created.toResponse())
            }
            put("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                val req = call.receive<UpdateTask>().validate()
                val updated = service.update(id, req)
                call.respond(HttpStatusCode.OK, updated.toResponse())
            }
        }
    }
}