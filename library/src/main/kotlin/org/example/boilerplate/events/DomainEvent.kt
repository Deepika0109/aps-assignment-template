package org.example.boilerplate.events

import org.example.boilerplate.JSON

/** ---- Domain Events (shared contract for CQRS) ---- */

data class TaskCreated(
    val eventId: String,
    val eventType: String = "TaskCreated",
    val aggregateId: String,          // task id (UUID as string)
    val occurredAt: String,           // ISO-8601 (Instant.toString())
    val name: String,
    val description: String?,
    val dueDate: String?,             // ISO-8601 string or null
    val assignees: List<String>,
    val createdAt: String             // ISO-8601
)

data class TaskUpdated(
    val eventId: String,
    val eventType: String = "TaskUpdated",
    val aggregateId: String,
    val occurredAt: String,           // ISO-8601
    val name: String?,                // null = unchanged
    val description: String?,         // null/blank handled on write side
    val dueDate: String?,             // null = unchanged
    val assigneesAdd: List<String> = emptyList(),
    val assigneesRem: List<String> = emptyList()
)

data class TaskDeleted(
    val eventId: String,
    val eventType: String = "TaskDeleted",
    val aggregateId: String,
    val occurredAt: String            // ISO-8601
)

/** ---- Tiny helper so producer/consumer use the same JSON path (uses template's JSON helper) ---- */
object EventJson {
    fun encode(event: Any): String = JSON.stringify(event)
    inline fun <reified T> decode(json: String): T = JSON.parse(json)
}
