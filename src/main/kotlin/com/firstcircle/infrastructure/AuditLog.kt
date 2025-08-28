package org.example.com.firstcircle.infrastructure

import org.example.com.firstcircle.domain.AuditLogEntry
import org.example.com.firstcircle.domain.Money
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.UUID

@Service
class AuditLog {
    private val entries = mutableListOf<AuditLogEntry>()
    private var previousHash = "INIT"

    @Synchronized
    fun record(
        operation: String,
        accountIds: List<UUID>,
        amount: Money,
    ) {
        val hash = "$operation|$accountIds|$amount|$previousHash".sha256()
        val entry =
            AuditLogEntry(
                operation = operation,
                accountIds = accountIds,
                previousHash = previousHash,
                hash = hash,
                amount = amount,
            )
        entries.add(entry)
        previousHash = hash
    }

    fun entries(): List<AuditLogEntry> = entries.toList()
}

fun String.sha256(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(this.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
