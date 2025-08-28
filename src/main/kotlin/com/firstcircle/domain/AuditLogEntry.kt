package org.example.com.firstcircle.domain

import java.time.Instant
import java.util.UUID

data class AuditLogEntry(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val operation: String,
    val accountIds: List<UUID>,
    val amount: Money,
    val previousHash: String,
    val hash: String,
)
