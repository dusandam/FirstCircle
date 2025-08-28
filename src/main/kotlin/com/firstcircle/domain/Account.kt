package org.example.com.firstcircle.domain

import java.time.Instant
import java.util.UUID

data class Account(
    val id: UUID = UUID.randomUUID(),
    val balance: Money,
    val ownerName: String? = null,
    val createdAt: Instant = Instant.now(),
    val accountNumber: String? = null,
) {
    fun deposit(amount: Money) = copy(balance = balance + amount)

    fun withdraw(amount: Money) = copy(balance = balance - amount)
}
