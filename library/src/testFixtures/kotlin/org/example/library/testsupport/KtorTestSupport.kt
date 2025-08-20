package org.example.library.testsupport

import io.ktor.server.application.*
import io.ktor.server.testing.*

/**
 * Run a Ktor test app with your provided module.
 * Usage in tests:
 *   testApp({ myModule() }) { /* use client */ }
 */
suspend fun testApp(
    module: Application.() -> Unit,
    block: suspend ApplicationTestBuilder.() -> Unit
) {
    testApplication {
        application { module() }
        block()
    }
}
