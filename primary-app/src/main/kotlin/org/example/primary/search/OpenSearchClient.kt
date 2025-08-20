package org.example.primary.search

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder

object OpenSearch {

    val client: OpenSearchClient by lazy {
        val hosts = (System.getenv("OPENSEARCH_HOSTS") ?: "http://localhost:9200")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }

        // Build the transport directly with HttpClient5
        val transport = ApacheHttpClient5TransportBuilder
            .builder(*hosts.map { HttpHost.create(it) }.toTypedArray())
            .setMapper(JacksonJsonpMapper())
            .build()

        OpenSearchClient(transport)
    }
}
