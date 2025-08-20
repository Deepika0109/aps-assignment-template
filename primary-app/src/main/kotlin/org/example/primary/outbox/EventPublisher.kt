package org.example.primary.outbox

import com.rabbitmq.client.ConnectionFactory
import org.example.boilerplate.events.EventJson
import java.nio.charset.StandardCharsets

object EventPublisher {

    private val exchange = System.getenv("RABBITMQ_EXCHANGE") ?: "tasks.events"  // keep simple
    private val factory by lazy {
        ConnectionFactory().apply {
            host = System.getenv("RABBITMQ_HOST") ?: "localhost"
            port = (System.getenv("RABBITMQ_PORT") ?: "5672").toInt()
            username = System.getenv("RABBITMQ_USER") ?: "guest"
            password = System.getenv("RABBITMQ_PASS") ?: "guest"
            isAutomaticRecoveryEnabled = true
        }
    }

    // lightweight singleton channel
    private val channel by lazy {
        val conn = factory.newConnection()
        conn.createChannel().apply {
            exchangeDeclare(exchange, "topic", true)
        }
    }

    fun publish(routingKey: String, event: Any) {
        val body = EventJson.encode(event).toByteArray(StandardCharsets.UTF_8)
        channel.basicPublish(exchange, routingKey, null, body)
    }
}
