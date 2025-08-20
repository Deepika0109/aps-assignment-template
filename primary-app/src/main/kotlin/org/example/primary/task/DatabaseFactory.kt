package org.example.primary.task

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseFactory {
    fun init(env: ApplicationEnvironment) {
        val cfg = env.config
        val hc = HikariConfig().apply {
            jdbcUrl = cfg.propertyOrNull("db.url")?.getString()
                ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
            username = cfg.propertyOrNull("db.user")?.getString()
                ?: System.getenv("DB_USER") ?: "postgres"
            password = cfg.propertyOrNull("db.password")?.getString()
                ?: System.getenv("DB_PASSWORD") ?: "postgres"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        }
        Database.connect(HikariDataSource(hc))
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED

        transaction {
            SchemaUtils.createMissingTablesAndColumns(TaskTable, TaskAssigneeTable)
        }
    }
}
