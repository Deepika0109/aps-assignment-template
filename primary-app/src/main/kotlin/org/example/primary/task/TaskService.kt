package org.example.primary.task

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import java.time.Instant
import java.util.UUID

class TaskService(
    private val repo: TaskRepository = TaskRepository(),
    private val os: OpenSearchClient = defaultOpenSearchClient()
) {
    fun create(req: CreateTask) = repo.create(req)

    fun get(id: UUID): Task {
        // 1) Try read model (OpenSearch)
        val doc = runCatching {
            val resp = os.get({ it.index("tasks").id(id.toString()) }, Map::class.java)
            if (resp.found()) resp.source() as Map<String, Any?> else null
        }.getOrNull()

        if (doc != null) {
            return mapOsDocToTask(doc)
        }

        // 2) Fallback to write model (DB)
        return repo.get(id) ?: throw NoSuchElementException()
    }

    fun update(id: UUID, req: UpdateTask) = repo.update(id, req)
    fun delete(id: UUID) = repo.delete(id)
}

/* ---- helpers ---- */

private fun defaultOpenSearchClient(): OpenSearchClient {
    val hosts = (System.getenv("OPENSEARCH_HOSTS") ?: "http://localhost:9200")
        .split(",").map { it.trim() }.filter { it.isNotBlank() }
    val transport = ApacheHttpClient5TransportBuilder
        .builder(*hosts.map { HttpHost.create(it) }.toTypedArray())
        .setMapper(JacksonJsonpMapper())
        .build()
    return OpenSearchClient(transport)
}

private fun mapOsDocToTask(src: Map<String, Any?>): Task {
    // The projector stores: id (String UUID), name, description, due_date (String?), assignees (List<String>), created_at (String)
    val idStr = src["id"]?.toString() ?: error("id missing in OS doc")
    val name = src["name"]?.toString() ?: error("name missing in OS doc")
    val description = src["description"]?.toString()
    val dueIso = src["due_date"] as String?
    val createdIso = src["created_at"]?.toString() ?: error("created_at missing in OS doc")
    val assignees = (src["assignees"] as? List<*>)?.map { it.toString() }?.toSet() ?: emptySet()

    return Task(
        id = UUID.fromString(idStr),
        name = name,
        description = description,
        dueDate = parseInstantOrNull(dueIso),
        createdAt = Instant.parse(createdIso),
        assignees = assignees
    )
}
