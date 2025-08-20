package org.example.boilerplate.rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties

class RabbitExchange(
    val name: String,
    private val channel: Channel,
) {
    /**
     * Publish a message on the exchange with the provided routing key.
     */
    fun publish(message: String, routingKey: String = "") {
        channel.basicPublish(name, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, message.toByteArray())
    }
}