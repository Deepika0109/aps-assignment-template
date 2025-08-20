package org.example.primary.outbox

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object OutboxPublisher {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // fetch a small batch of unpublished events
                    val batch: List<ResultRow> = transaction {
                        OutboxEvents.select { OutboxEvents.published eq false }
                            .orderBy(OutboxEvents.occurredAt to SortOrder.ASC)
                            .limit(50)
                            .toList()
                    }

                    if (batch.isEmpty()) {
                        delay(1000) // back off when idle
                        continue
                    }

                    // publish each and mark as published
                    transaction {
                        batch.forEach { row ->
                            val id = row[OutboxEvents.id]
                            val type = row[OutboxEvents.type]          // e.g., "TaskCreated"
                            val payload = row[OutboxEvents.payload]

                            val routingKey = "task.${type.lowercase()}" // task.created|updated|deleted
                            EventPublisher.publish(routingKey, payload)

                            OutboxEvents.update({ OutboxEvents.id eq id }) {
                                it[published] = true
                                it[publishedAt] = Instant.now().toString()
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    break
                } catch (t: Throwable) {
                    t.printStackTrace()
                    delay(1000) // simple retry/backoff
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
