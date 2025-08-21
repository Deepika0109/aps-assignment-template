package org.example.primary

import org.example.boilerplate.errors.ValidationException
import org.example.primary.task.*
import org.example.primary.validation.CompositeValidator
import org.example.primary.validation.ValidationRegistry
import org.example.primary.validation.validateWithRegistry
import kotlin.test.*

class ValidatorTest {

    @BeforeTest
    fun setup() {
        // Register the  validators
        ValidationRegistry.register(
            CreateTask::class,
            CompositeValidator(NameRequiredNonBlank, DueDateIsoIfPresentOnCreate)
        )
        ValidationRegistry.register(
            UpdateTask::class,
            CompositeValidator(
                UpdateNameNonBlankIfPresent,
                DueDateIsoIfPresentOnUpdate,
                AssigneePatchStructureIfPresent
            )
        )
    }

    /* ---------- CreateTask tests ---------- */

    @Test
    fun `create - missing name should fail`() {
        val req = CreateTask(name = null, description = null, due_date = null, assignees = null)
        val ex = assertFailsWith<ValidationException> { req.validateWithRegistry() }
        assertTrue(ex.body.fields.any { it.field == "name" && it.error.contains("required") })
    }

    @Test
    fun `create - blank name should fail`() {
        val req = CreateTask(name = "   ", description = null, due_date = null, assignees = null)
        val ex = assertFailsWith<ValidationException> { req.validateWithRegistry() }
        assertTrue(ex.body.fields.any { it.field == "name" })
    }


    @Test
    fun `create - minimal valid should pass`() {
        val req = CreateTask(name = "Task A", description = null, due_date = null, assignees = null)
        val result = runCatching { req.validateWithRegistry() }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `create - valid ISO due_date should pass`() {
        val req = CreateTask(name = "Task A", description = null, due_date = "2025-12-31T10:00:00Z", assignees = null)
        val result = runCatching { req.validateWithRegistry() }
        assertTrue(result.isSuccess)
    }

    /* ---------- UpdateTask tests ---------- */

    @Test
    fun `update - name present but blank should fail`() {
        val req = UpdateTask(name = "   ", description = null, assignees = null, due_date = null)
        val ex = assertFailsWith<ValidationException> { req.validateWithRegistry() }
        assertTrue(ex.body.fields.any { it.field == "name" })
    }


    @Test
    fun `update - assignees missing rem should fail`() {
        val patch = AssigneePatch(add = listOf("a@x.com"), rem = null)
        val req = UpdateTask(name = null, description = null, assignees = patch, due_date = null)
        val ex = assertFailsWith<ValidationException> { req.validateWithRegistry() }
        assertTrue(ex.body.fields.any { it.field == "assignees" })
    }

    @Test
    fun `update - valid patch should pass`() {
        val patch = AssigneePatch(add = listOf("a@x.com"), rem = listOf("b@y.com"))
        val req = UpdateTask(name = "Renamed", description = null, assignees = patch, due_date = "2025-12-31T10:00:00Z")
        val result = runCatching { req.validateWithRegistry() }
        assertTrue(result.isSuccess)
    }
}
