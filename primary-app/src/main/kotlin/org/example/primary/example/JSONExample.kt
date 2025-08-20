package org.example.primary.example

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.boilerplate.JSON
import org.example.boilerplate.logging.LoggerFactory

private data class ChecklistItem(
    @JsonProperty("title")
    val title: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("completed")
    val completed: Boolean,
)

/**
 * The following function demonstrates how to use the provided boilerplate JSON functions.
 * You may or may not need it, but it can help make working with JSON's a bit more convenient.
 */
@Suppress("UNUSED")
fun runJSONExample() {
    val logger = LoggerFactory.create(::runJSONExample.name)

    val original = ChecklistItem("Buy birthday present!", "James will turn 29 this month", false)
    val serialized = JSON.stringify(original)
    logger.info { "Serialized checklist item: $serialized" }

    val deserialized = JSON.parse<ChecklistItem>(serialized)
    logger.info { "Deserialized checklist item: $deserialized" }

    val plain = hashMapOf<String, Any>(
        "title" to original.title,
        "description" to original.description,
        "completed" to original.completed
    )

    val converted = JSON.valueToType<ChecklistItem>(plain)
    logger.info { "Converted checklist item: $converted" }
}