package org.example.primary.task

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TaskTable : Table("task") {
    val id = uuid("id")
    val name = text("name")
    val description = text("description").nullable()
    val dueDate = timestamp("due_date").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object TaskAssigneeTable : Table("task_assignee") {
    val taskId = uuid("task_id").references(TaskTable.id, onDelete = ReferenceOption.CASCADE)
    val email = text("email")

    override val primaryKey = PrimaryKey(taskId, email)
}
