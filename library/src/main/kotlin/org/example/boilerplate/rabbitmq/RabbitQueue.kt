package org.example.boilerplate.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.example.boilerplate.logging.LoggerFactory
import org.example.boilerplate.logging.create
import java.util.*

class RabbitQueue(
    val name: String,
    private val channel: Channel,
) {
    private val logger = LoggerFactory.create<RabbitQueue>()

    /**
     * Bind the queue to the provided [RabbitExchange] with using the supplied [routingKey].
     */
    fun bindExchange(exchange: RabbitExchange, routingKey: String = "") {
        if (exchange.name.isNotBlank()) {
            channel.queueBind(name, exchange.name, routingKey)
        }
    }

    /**
     * Start consuming the queue and receive new [RabbitQueueObject] asynchronously via the [callback].
     */
    fun consume(callback: (obj: RabbitQueueObject) -> Unit) {
        channel.basicConsume(name, false, UUID.randomUUID().toString(), object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray,
            ) {
                try {
                    callback(RabbitQueueObject(String(body), envelope.routingKey, envelope.deliveryTag))
                } catch (e: Exception) {
                    logger.error(e) { "Exception occurred while handling delivery; deliveryTag=`${envelope.deliveryTag}`" }
                    channel.basicNack(envelope.deliveryTag, false, false)
                }
            }
        })
    }

    /**
     * Acknowledge the RabbitMQ message with the provided [deliveryTag]
     */
    fun ack(deliveryTag: Long, multiple: Boolean = false) {
        channel.basicAck(deliveryTag, multiple)
    }

    /**
     * Negatively acknowledge the RabbitMQ message with the provided [deliveryTag]
     */
    fun nack(deliveryTag: Long, multiple: Boolean = false, requeue: Boolean = true) {
        channel.basicNack(deliveryTag, multiple, requeue)
    }
}