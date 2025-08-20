package org.example.secondary

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.hc.core5.http.HttpHost
import org.example.boilerplate.JSON
import org.example.boilerplate.Server
import org.example.boilerplate.events.EventJson
import org.example.boilerplate.events.TaskCreated
import org.example.boilerplate.events.TaskDeleted
import org.example.boilerplate.events.TaskUpdated
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.DateProperty
import org.opensearch.client.opensearch._types.mapping.KeywordProperty
import org.opensearch.client.opensearch._types.mapping.Property
import org.opensearch.client.opensearch._types.mapping.TextProperty
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

fun main() = Server.run(port = 9044) { entrypoint() }

fun Application.entrypoint() {
    // lightweight health
    routing { get("/health") { call.respondText("secondary OK") } }

    val os = openSearchClientFromEnv()
    ensureIndex(os, "tasks")

    val cf = ConnectionFactory().apply {
        host = System.getenv("RABBITMQ_HOST") ?: "localhost"
        port = (System.getenv("RABBITMQ_PORT") ?: "5672").toInt()
        username = System.getenv("RABBITMQ_USER") ?: "guest"
        password = System.getenv("RABBITMQ_PASS") ?: "guest"
        isAutomaticRecoveryEnabled = true
    }
    val conn = cf.newConnection()
    val ch = conn.createChannel()

    val exchange = System.getenv("RABBITMQ_EXCHANGE") ?: "tasks.events"
    val queue = System.getenv("RABBITMQ_QUEUE") ?: "tasks.events.q"

    ch.exchangeDeclare(exchange, "topic", true)
    ch.queueDeclare(queue, true, false, false, null)
    ch.queueBind(queue, exchange, "task.#")
    ch.basicQos(50) // avoid huge memory spikes

    // consumer thread
    thread(name = "secondary-consumer", isDaemon = true) {
        ch.basicConsume(queue, false, object : DefaultConsumer(ch) {
            override fun handleDelivery(tag: String?, env: Envelope, props: AMQP.BasicProperties?, body: ByteArray) {
                val json = String(body, StandardCharsets.UTF_8)
                try {
                    // decoding eventType from JSON payload
                    val type = runCatching { JSON.valueToType<Map<String, Any?>>(json)["eventType"]?.toString() }
                        .getOrNull()

                    when (type) {
                        "TaskCreated" -> {
                            val e = EventJson.decode<TaskCreated>(json)
                            val doc = mapOf(
                                "id" to e.aggregateId,
                                "name" to e.name,
                                "description" to e.description,
                                "due_date" to e.dueDate,
                                "assignees" to e.assignees,
                                "created_at" to e.createdAt
                            )
                            os.index(
                                IndexRequest.Builder<Map<String, Any?>>()
                                    .index("tasks")
                                    .id(e.aggregateId)
                                    .document(doc)
                                    .build()
                            )
                        }
                        "TaskUpdated" -> {
                            val e = EventJson.decode<TaskUpdated>(json)
                            val get = os.get({ it.index("tasks").id(e.aggregateId) }, Map::class.java)
                            val cur = if (get.found()) (get.source() as Map<String, Any?>) else emptyMap()

                            val assignees = (cur["assignees"] as? List<*>)?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()
                            assignees.addAll(e.assigneesAdd)
                            assignees.removeAll(e.assigneesRem.toSet())

                            val merged = mapOf(
                                "id" to (cur["id"] ?: e.aggregateId),
                                "name" to (e.name ?: cur["name"]),
                                "description" to (e.description ?: cur["description"]),
                                "due_date" to (e.dueDate ?: cur["due_date"]),
                                "assignees" to assignees.toList(),
                                "created_at" to (cur["created_at"])
                            )
                            os.index(
                                IndexRequest.Builder<Map<String, Any?>>()
                                    .index("tasks")
                                    .id(e.aggregateId)
                                    .document(merged)
                                    .build()
                            )
                        }
                        "TaskDeleted" -> {
                            val e = EventJson.decode<TaskDeleted>(json)
                            os.delete { it.index("tasks").id(e.aggregateId) }
                        }
                        else -> {
                            // unknown event -> drop or DLQ;
                            throw IllegalArgumentException("Unknown eventType: $type")
                        }
                    }

                    // success
                    ch.basicAck(env.deliveryTag, false)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    // send to DLQ if bound; do not requeue to avoid poison loops
                    ch.basicNack(env.deliveryTag, false, /* requeue = */ false)
                }
            }
        })
    }

    // clean shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        try { ch.close() } catch (_: Exception) {}
        try { conn.close() } catch (_: Exception) {}
        // OS transport in a Closeable
    }
}

private fun openSearchClientFromEnv(): OpenSearchClient {
    val hosts = (System.getenv("OPENSEARCH_HOSTS") ?: "http://localhost:9200")
        .split(",").map { it.trim() }.filter { it.isNotBlank() }
    val transport = ApacheHttpClient5TransportBuilder
        .builder(*hosts.map { HttpHost.create(it) }.toTypedArray())
        .setMapper(JacksonJsonpMapper())
        .build()
    return OpenSearchClient(transport)
}

private fun ensureIndex(os: OpenSearchClient, index: String) {
    val exists = runCatching { os.indices().exists { it.index(index) }.value() }.getOrDefault(false)
    if (!exists) {
        // Explicit builders for OpenSearch mappings
        val mapping = TypeMapping.Builder()
            .properties("id", Property.Builder().keyword(KeywordProperty.Builder().build()).build())
            .properties("name", Property.Builder().text(TextProperty.Builder().build()).build())
            .properties("description", Property.Builder().text(TextProperty.Builder().build()).build())
            .properties("due_date", Property.Builder().date(DateProperty.Builder().build()).build())
            .properties("assignees", Property.Builder().keyword(KeywordProperty.Builder().build()).build())
            .properties("created_at", Property.Builder().date(DateProperty.Builder().build()).build())
            .build()

        os.indices().create(
            CreateIndexRequest.Builder()
                .index(index)
                .mappings(mapping)
                .build()
        )
    }
}
