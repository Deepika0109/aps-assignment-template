package org.example.primary.task

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.UUID

data class Task(
    val id: UUID,
    val name: String,
    val description: String?,
    val dueDate: Instant?,
    val createdAt: Instant,
    val assignees: Set<String> = emptySet()
)

data class TaskResponse(
    val id: String,
    val name: String,
    val description: String?,
    val due_date: String?,
    val assignees: List<String>,
    val created_at: String
)

data class CreateTask(
    val name: String?,
    val description: String?,
    val due_date: String?,
    val assignees: List<String>? = null
)

data class AssigneePatch(val add: List<String>?, val rem: List<String>?)
data class UpdateTask(
    val name: String?,
    val description: String?,
    val assignees: AssigneePatch?,
    val due_date: String?
)

fun CreateTask.validate(): CreateTask {
    if (name.isNullOrBlank()) throw IllegalArgumentException("name is required and cannot be blank")
    if (due_date != null) parseInstantOrNull(due_date) // throws for bad format
    return this
}

fun UpdateTask.validate(): UpdateTask {
    if (name != null && name.isBlank()) throw IllegalArgumentException("name cannot be blank")
    if (due_date != null) parseInstantOrNull(due_date)
    return this
}

fun parseInstantOrNull(s: String?): Instant? {
    if (s == null) return null
    return try { Instant.parse(s) } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("Invalid timestamp")
    }
}

fun Task.toResponse() = TaskResponse(
    id = id.toString(),
    name = name,
    description = description,
    due_date = dueDate?.toString(),
    assignees = assignees.sorted(),
    created_at = createdAt.atOffset(ZoneOffset.UTC).toInstant().toString()
)
