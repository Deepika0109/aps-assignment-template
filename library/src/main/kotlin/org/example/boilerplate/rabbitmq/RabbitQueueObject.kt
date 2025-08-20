package org.example.boilerplate.rabbitmq

data class RabbitQueueObject(
    val payload: String,
    val routingKey: String,
    val deliveryTag: Long,
)