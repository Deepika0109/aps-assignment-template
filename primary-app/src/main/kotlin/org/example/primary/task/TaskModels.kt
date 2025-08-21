package org.example.primary.task

import org.example.boilerplate.errors.FieldError
import org.example.boilerplate.errors.ValidationException
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
    val errors = mutableListOf<FieldError>()

    if (name.isNullOrBlank()) {
        errors += FieldError("name", "required_non_blank")
    }
    if (due_date != null && parseInstantOrNull(due_date) == null) {
        errors += FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z")
    }
    if (errors.isNotEmpty()) throw ValidationException(errors)
    return this
}

fun UpdateTask.validate(): UpdateTask {
    val errors = mutableListOf<FieldError>()

    if (name != null && name.isBlank()) {
        errors += FieldError("name", "required_non_blank_if_present")
    }
    if (due_date != null && parseInstantOrNull(due_date) == null) {
        errors += FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z")
    }
    assignees?.let {
        // If provided, must have both keys (even if the lists are empty)
        if (it.add == null || it.rem == null) {
            errors += FieldError("assignees", "patch_must_contain_add_and_rem")
        }
    }
    if (errors.isNotEmpty()) throw ValidationException(errors)
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
