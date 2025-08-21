package org.example.boilerplate.errors

import io.ktor.http.*

data class FieldError(val field: String, val error: String, val hint: String? = null)

data class ApiError(
    val code: String,
    val message: String,
    val fields: List<FieldError> = emptyList()
)

open class ApiException(
    val status: HttpStatusCode,
    val body: ApiError
) : RuntimeException(body.message)

class ValidationException(fields: List<FieldError>) : ApiException(
    HttpStatusCode.BadRequest,
    ApiError(code = "validation_error", message = "Invalid request payload.", fields = fields)
)
