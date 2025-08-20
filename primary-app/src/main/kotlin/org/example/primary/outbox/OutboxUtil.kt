package org.example.primary.outbox

import org.example.boilerplate.events.EventJson
import org.example.boilerplate.events.TaskCreated
import org.example.boilerplate.events.TaskDeleted
import org.example.boilerplate.events.TaskUpdated
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Table schema for outbox events.
 */
object OutboxEvents : Table("outbox_events") {
    val id = uuid("id")                    // eventId
    val aggregateId = uuid("aggregate_id") // task id
    val type = varchar("type", 100)        // "TaskCreated" | "TaskUpdated" | "TaskDeleted"
    val payload = text("payload")          // JSON payload
    val occurredAt = varchar("occurred_at", 64)
    val published = bool("published").default(false)
    val publishedAt = varchar("published_at", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Outbox helper for inserting events in a single transaction with your write model.
 */
object Outbox {

    fun insert(event: Any) {
        when (event) {
            is TaskCreated -> insertCreated(event)
            is TaskUpdated -> insertUpdated(event)
            is TaskDeleted -> insertDeleted(event)
            else -> error("Unsupported event type: ${event::class.qualifiedName}")
        }
    }

    private fun insertCreated(e: TaskCreated) = transaction {
        OutboxEvents.insert {
            it[id] = UUID.fromString(e.eventId)
            it[aggregateId] = UUID.fromString(e.aggregateId)
            it[type] = "TaskCreated"
            it[occurredAt] = e.occurredAt
            it[payload] = EventJson.encode(e)
            it[published] = false
            it[publishedAt] = null
        }
    }

    private fun insertUpdated(e: TaskUpdated) = transaction {
        OutboxEvents.insert {
            it[id] = UUID.fromString(e.eventId)
            it[aggregateId] = UUID.fromString(e.aggregateId)
            it[type] = "TaskUpdated"
            it[occurredAt] = e.occurredAt
            it[payload] = EventJson.encode(e)
            it[published] = false
            it[publishedAt] = null
        }
    }

    private fun insertDeleted(e: TaskDeleted) = transaction {
        OutboxEvents.insert {
            it[id] = UUID.fromString(e.eventId)
            it[aggregateId] = UUID.fromString(e.aggregateId)
            it[type] = "TaskDeleted"
            it[occurredAt] = e.occurredAt
            it[payload] = EventJson.encode(e)
            it[published] = false
            it[publishedAt] = null
        }
    }
}
