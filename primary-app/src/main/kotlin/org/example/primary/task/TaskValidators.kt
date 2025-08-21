package org.example.primary.task

import org.example.boilerplate.errors.FieldError
import org.example.primary.validation.Validator

/** Re-usable rules (small, focused). */
object NameRequiredNonBlank : Validator<CreateTask> {
    override fun validate(v: CreateTask) =
        if (v.name.isNullOrBlank()) listOf(FieldError("name", "required_non_blank")) else emptyList()
}

object DueDateIsoIfPresentOnCreate : Validator<CreateTask> {
    override fun validate(v: CreateTask): List<FieldError> {
        if (v.due_date != null && v.due_date.isBlank()) {
            return listOf(FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z"))
        }
        return if (v.due_date != null && parseInstantOrNull(v.due_date) == null)
            listOf(FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z"))
        else emptyList()
    }
}

object UpdateNameNonBlankIfPresent : Validator<UpdateTask> {
    override fun validate(v: UpdateTask) =
        if (v.name != null && v.name.isBlank())
            listOf(FieldError("name", "required_non_blank_if_present")) else emptyList()
}

object DueDateIsoIfPresentOnUpdate : Validator<UpdateTask> {
    override fun validate(v: UpdateTask): List<FieldError> {
        if (v.due_date != null && v.due_date.isBlank()) {
            return listOf(FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z"))
        }
        return if (v.due_date != null && parseInstantOrNull(v.due_date) == null)
            listOf(FieldError("due_date", "invalid_iso8601", "Use e.g. 2025-12-31T10:00:00Z"))
        else emptyList()
    }
}

object AssigneePatchStructureIfPresent : Validator<UpdateTask> {
    override fun validate(v: UpdateTask) =
        if (v.assignees != null && (v.assignees.add == null || v.assignees.rem == null))
            listOf(FieldError("assignees", "patch_must_contain_add_and_rem"))
        else emptyList()
}
