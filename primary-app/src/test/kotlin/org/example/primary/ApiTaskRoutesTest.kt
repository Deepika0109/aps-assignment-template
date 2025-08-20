package org.example.primary

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import org.example.primary.task.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiTaskRoutesTest {

    private fun Application.installWith(service: TaskService) {
        install(ContentNegotiation) { jackson() }   // same JSON plugin as prod
        installTaskRoutes(service)                  // <- your routing from Application.kt
    }

    @Test
    fun `POST create returns 200 and body`() = testApplication {
        val service = mockk<TaskService> {
            every { create(any()) } answers {
                val req = firstArg<CreateTask>()
                Task(
                    id = UUID.randomUUID(),
                    name = req.name!!,
                    description = req.description,
                    dueDate = req.due_date?.let(Instant::parse),
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    assignees = req.assignees?.toSet() ?: emptySet()
                )
            }
        }

        application {
            installWith(service)   // block form; returns Unit (correct)
        }

        val res = client.post("/api/v2/task") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "My API Task",
                  "description": "first run",
                  "due_date": "2025-12-01T10:00:00Z",
                  "assignees": ["a@b.com","b@c.com"]
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("My API Task"))
        assertTrue(body.contains("a@b.com"))
    }

    @Test
    fun `GET with bad id returns 400`() = testApplication {
        val service = mockk<TaskService>(relaxed = true)

        application {
            installWith(service)
        }

        val res = client.get("/api/v2/task/not-a-uuid")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `PUT update with assignee patch returns 200`() = testApplication {
        val service = mockk<TaskService> {
            every { update(any(), any()) } answers {
                val id = firstArg<UUID>()
                val req = secondArg<UpdateTask>()
                Task(
                    id = id,
                    name = req.name ?: "Existing",
                    description = req.description,
                    dueDate = req.due_date?.let(Instant::parse),
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    assignees = (req.assignees?.add?.toSet() ?: setOf("keep@x.com")) -
                            (req.assignees?.rem?.toSet() ?: emptySet())
                )
            }
        }

        application {
            installWith(service)
        }

        val id = UUID.randomUUID()
        val res = client.put("/api/v2/task/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{ "name":"Updated", "assignees": { "add": ["c@d.com"], "rem": ["old@z.com"] } }""")
        }

        assertEquals(HttpStatusCode.OK, res.status)
        val json = res.bodyAsText()
        assertTrue(json.contains("Updated"))
        assertTrue(json.contains("c@d.com"))
    }

    @Test
    fun `DELETE returns 204`() = testApplication {
        val service = mockk<TaskService> { every { delete(any()) } returns Unit }

        application {
            installWith(service)
        }

        val id = UUID.randomUUID()
        val res = client.delete("/api/v2/task/$id")
        assertEquals(HttpStatusCode.NoContent, res.status)
    }
}
