package org.example.boilerplate.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.util.concurrent.Executors

class RabbitClient private constructor(
    private val native: Connection,
) {
    private val defaultChannel = native.createChannel()

    /**
     * Create a new [RabbitQueue] instance using a dedicated [com.rabbitmq.client.Channel].
     */
    fun createQueue(
        name: String,
        durable: Boolean = true,
        exclusive: Boolean = false,
        autoDelete: Boolean = false,
    ): RabbitQueue {
        val queueChannel = native.createChannel()
        queueChannel.queueDeclare(name, durable, exclusive, autoDelete, null)

        return RabbitQueue(name, queueChannel)
    }

    /**
     * Create a new [RabbitExchange] instance that refers to the default topic exchange that is present in RabbitMQ.
     */
    fun defaultTopicExchange(): RabbitExchange {
        return RabbitExchange("amq.topic", defaultChannel)
    }

    /**
     * Create a new [RabbitExchange] instance that refers to the default direct exchange that is present in RabbitMQ.
     */
    fun defaultDirectExchange(): RabbitExchange {
        return RabbitExchange("amq.direct", defaultChannel)
    }

    companion object {
        /**
         * Create a new [RabbitClient] with a cached thread pool set as its shared executor.
         */
        fun create(uri: String): RabbitClient {
            val factory = ConnectionFactory()
            factory.setUri(uri)
            factory.setSharedExecutor(Executors.newCachedThreadPool())

            return RabbitClient(factory.newConnection())
        }
    }
}