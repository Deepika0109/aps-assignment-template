package org.example.primary.example

import org.example.boilerplate.Clients
import org.example.boilerplate.logging.LoggerFactory

/**
 * The following function demonstrates how to use the provided boilerplate RabbitMQ client.
 * If you choose to implement an extra feature using RabbitMQ, hopefully this makes it a more convenient to work with.
 */
@Suppress("UNUSED")
fun runRabbitMQExample() {
    val logger = LoggerFactory.create(::runRabbitMQExample.name)

    val exchange = Clients.rabbitMQ.defaultTopicExchange()

    val taskProjectionQueue = Clients.rabbitMQ.createQueue("task_projection_queue").apply {
        bindExchange(exchange, routingKey = "task.created")
        bindExchange(exchange, routingKey = "task.updated.*")
    }

    taskProjectionQueue.consume { obj ->
        logger.info { "Task projection queue (${obj.routingKey}): ${obj.payload}" }
        taskProjectionQueue.ack(obj.deliveryTag)
    }

    val taskNotificationQueue = Clients.rabbitMQ.createQueue("task_notification_queue").apply {
        bindExchange(exchange, routingKey = "task.#")
    }

    taskNotificationQueue.consume { obj ->
        logger.info { "Task notification queue (${obj.routingKey}): ${obj.payload}" }
        taskNotificationQueue.ack(obj.deliveryTag)
    }

    exchange.publish("Task has been created!", routingKey = "task.created")
    exchange.publish("Task has been updated!", routingKey = "task.updated.name")
}
