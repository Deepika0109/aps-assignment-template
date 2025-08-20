package org.example.primary.task

import org.example.primary.outbox.Outbox
import org.example.boilerplate.events.TaskCreated
import org.example.boilerplate.events.TaskUpdated
import org.example.boilerplate.events.TaskDeleted
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class TaskRepository {

    fun create(req: CreateTask): Task = transaction {
        val id = UUID.randomUUID()
        val now: Instant = Instant.now()

        val name = requireNotNull(req.name?.trim()) { "name is required" }
        require(name.isNotBlank()) { "name cannot be blank" }
        val desc = req.description?.takeIf { it.isNotBlank() }
        val due: Instant? = req.due_date?.let { parseInstantOrNull(it) }

        // 1) Write model
        TaskTable.insert {
            it[TaskTable.id] = id
            it[TaskTable.name] = name
            it[TaskTable.description] = desc
            it[TaskTable.dueDate] = due
            it[TaskTable.createdAt] = now
        }

        val assignees: Set<String> = req.assignees.orEmpty().toSet()
        if (assignees.isNotEmpty()) {
            TaskAssigneeTable.batchInsert(assignees) { email ->
                this[TaskAssigneeTable.taskId] = id
                this[TaskAssigneeTable.email] = email
            }
        }

        // Build the return object
        val created = Task(
            id = id,
            name = name,
            description = desc,
            dueDate = due,
            createdAt = now,
            assignees = assignees
        )

        // 2) Enqueue domain event
        Outbox.insert(
            TaskCreated(
                eventId = UUID.randomUUID().toString(),
                aggregateId = id.toString(),
                occurredAt = now.toString(),
                name = created.name,
                description = created.description,
                dueDate = created.dueDate?.toString(),
                assignees = created.assignees.toList(),
                createdAt = created.createdAt.toString()
            )
        )

        created
    }

    fun get(id: UUID): Task? = transaction {
        val row = TaskTable
            .select { TaskTable.id eq id }
            .firstOrNull() ?: return@transaction null

        val assignees: Set<String> = TaskAssigneeTable
            .slice(TaskAssigneeTable.email)
            .select { TaskAssigneeTable.taskId eq id }
            .map { it[TaskAssigneeTable.email] }
            .toSet()

        Task(
            id = row[TaskTable.id],
            name = row[TaskTable.name],
            description = row[TaskTable.description],
            dueDate = row[TaskTable.dueDate],        // Instant?
            createdAt = row[TaskTable.createdAt],    // Instant
            assignees = assignees
        )
    }

    fun update(id: UUID, req: UpdateTask): Task = transaction {
        val current = get(id) ?: throw NoSuchElementException()

        val newName = req.name?.trim() ?: current.name
        require(newName.isNotBlank()) { "name cannot be blank" }

        val newDesc = when (req.description) {
            null -> current.description
            "" -> null
            else -> req.description
        }

        val newDue: Instant? =
            if (req.due_date != null) parseInstantOrNull(req.due_date) else current.dueDate

        // 1) Write model
        TaskTable.update({ TaskTable.id eq id }) {
            it[TaskTable.name] = newName
            it[TaskTable.description] = newDesc
            it[TaskTable.dueDate] = newDue
        }

        // Assignees delta
        req.assignees?.add?.forEach { email ->
            TaskAssigneeTable.insertIgnore {
                it[TaskAssigneeTable.taskId] = id
                it[TaskAssigneeTable.email] = email
            }
        }
        req.assignees?.rem?.forEach { email ->
            TaskAssigneeTable.deleteWhere {
                (TaskAssigneeTable.taskId eq id) and (TaskAssigneeTable.email eq email)
            }
        }

        val updated = Task(
            id = id,
            name = newName,
            description = newDesc,
            dueDate = newDue,
            createdAt = current.createdAt,
            assignees = (current.assignees + (req.assignees?.add ?: emptyList()))
                .toMutableSet()
                .apply { removeAll(req.assignees?.rem ?: emptyList()) }
        )

        // 2) Enqueue domain event (string timestamps for events)
        Outbox.insert(
            TaskUpdated(
                eventId = UUID.randomUUID().toString(),
                aggregateId = id.toString(),
                occurredAt = Instant.now().toString(),     // Instant -> String
                name = req.name,
                description = req.description,
                dueDate = req.due_date,                    // already String? in request DTO
                assigneesAdd = req.assignees?.add ?: emptyList(),
                assigneesRem = req.assignees?.rem ?: emptyList()
            )
        )

        updated
    }

    fun delete(id: UUID) = transaction {
        // ensure existence
        get(id) ?: throw NoSuchElementException()

        // 1) Write model
        TaskTable.deleteWhere { TaskTable.id eq id }

        // 2) Enqueue domain event (string timestamps for events)
        Outbox.insert(
            TaskDeleted(
                eventId = UUID.randomUUID().toString(),
                aggregateId = id.toString(),
                occurredAt = Instant.now().toString()
            )
        )
    }
}
