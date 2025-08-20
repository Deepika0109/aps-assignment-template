package org.example.boilerplate

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

object JSON {
    val mapper: ObjectMapper = ObjectMapper()
        .findAndRegisterModules()
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    /**
     * Utility function to stringify any value to a JSON string
     */
    fun <T> stringify(value: T, pretty: Boolean = false): String {
        return if (pretty) {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        } else {
            mapper.writeValueAsString(value)
        }
    }

    /**
     * Utility function to parse a JSON string to a class
     */
    inline fun <reified T : Any> parse(value: String): T {
        return mapper.readValue(value, object : TypeReference<T>() {})
    }

    /**
     * Utility function to parse a class to another class.
     * Mostly used for raw HashMap mapping to an actual class.
     */
    inline fun <reified T : Any?> valueToType(value: Any?): T {
        return valueToType(value, object : TypeReference<T>() {})
    }

    /**
     * Utility function to parse a class to another class.
     * Mostly used for raw HashMap mapping to an actual class.
     */
    fun <T : Any?> valueToType(value: Any?, typeReference: TypeReference<T>): T {
        return mapper.convertValue(value, typeReference)
    }

    /**
     * Utility function to parse a value to a [JsonNode].
     */
    fun <T : Any> valueToNode(value: T): JsonNode {
        return mapper.valueToTree(value)
    }
}