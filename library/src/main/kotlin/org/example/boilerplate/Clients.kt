package org.example.boilerplate

import org.apache.hc.core5.http.HttpHost
import org.example.boilerplate.rabbitmq.RabbitClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder

object Clients {
    /**
     * Lazy to make sure RabbitMQ is not required to be running actively in a Docker container.
     */
    val rabbitMQ by lazy { RabbitClient.Companion.create("amqp://localhost:5672") }

    /**
     * Lazy to make sure OpenSearch is not required to be running actively in a Docker container.
     */
    val opensearchClient by lazy {
        OpenSearchClient(
            ApacheHttpClient5TransportBuilder
                .builder(HttpHost("localhost", 9200))
                .setMapper(JacksonJsonpMapper(JSON.mapper))
                .build()
        )
    }
}