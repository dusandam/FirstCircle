package com.firstcircle.service

import com.firstcircle.domain.Money
import com.firstcircle.infrastructure.AuditLog
import com.firstcircle.infrastructure.IdempotencyStore
import com.firstcircle.infrastructure.repository.InMemoryAccountRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.UUID

class BankingServiceTest :
    StringSpec({
        "create account with initial deposit" {
            val service = createService()
            val account = service.createAccount(UUID.randomUUID(), Money(BigDecimal(200)))

            service.getBalance(account.id) shouldBe Money(BigDecimal(200))
        }

        "deposit increases balance" {
            val service = createService()
            val account = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            service.deposit(UUID.randomUUID(), account.id, Money(BigDecimal(50)))
            service.getBalance(account.id) shouldBe Money(BigDecimal(150))
        }

        "withdraw decreases balance" {
            val service = createService()
            val account = service.createAccount(UUID.randomUUID(), Money(BigDecimal(200)))

            service.withdraw(UUID.randomUUID(), account.id, Money(BigDecimal(75)))
            service.getBalance(account.id) shouldBe Money(BigDecimal(125))
        }

        "transfer moves money between accounts" {
            val service = createService()
            val acc1 = service.createAccount(UUID.randomUUID(), Money(BigDecimal(300)))
            val acc2 = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            service.transfer(UUID.randomUUID(), acc1.id, acc2.id, Money(BigDecimal(50)))

            service.getBalance(acc1.id) shouldBe Money(BigDecimal(250))
            service.getBalance(acc2.id) shouldBe Money(BigDecimal(150))
        }

        "get balance returns correct value after multiple operations" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(500)))

            service.deposit(UUID.randomUUID(), acc.id, Money(BigDecimal(200)))
            service.withdraw(UUID.randomUUID(), acc.id, Money(BigDecimal(100)))
            service.deposit(UUID.randomUUID(), acc.id, Money(BigDecimal(50)))

            service.getBalance(acc.id) shouldBe Money(BigDecimal(650))
        }

        // ======= Enhanced tests =======

        "cannot withdraw more than balance" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            shouldThrow<IllegalArgumentException> {
                service.withdraw(UUID.randomUUID(), acc.id, Money(BigDecimal(200)))
            }
        }

        "cannot transfer to self" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            shouldThrow<IllegalArgumentException> {
                service.transfer(UUID.randomUUID(), acc.id, acc.id, Money(BigDecimal(50)))
            }
        }

        "idempotency prevents double execution" {
            val service = createService()
            val key = UUID.randomUUID()
            val acc = service.createAccount(key, Money(BigDecimal(100)))

            // retry with same idempotency key → balance unchanged
            val retry = service.createAccount(key, Money(BigDecimal(100)))
            retry.id shouldBe acc.id
            service.getBalance(acc.id) shouldBe Money(BigDecimal(100))
        }

        "audit log contains all operations" {
            val repo = InMemoryAccountRepository()
            val idempotency = IdempotencyStore()
            val audit = AuditLog()
            val service = BankingService(repo, idempotency, audit)

            val acc1 = service.createAccount(UUID.randomUUID(), Money(BigDecimal(200)))
            val acc2 = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            service.deposit(UUID.randomUUID(), acc1.id, Money(BigDecimal(50)))
            service.withdraw(UUID.randomUUID(), acc2.id, Money(BigDecimal(20)))
            service.transfer(UUID.randomUUID(), acc1.id, acc2.id, Money(BigDecimal(30)))

            val logs = audit.entries()
            logs.map { it.operation } shouldBe
                listOf(
                    "CREATE_ACCOUNT",
                    "CREATE_ACCOUNT",
                    "DEPOSIT",
                    "WITHDRAW",
                    "TRANSFER",
                )
        }

        "cannot deposit negative amount" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            shouldThrow<IllegalArgumentException> {
                service.deposit(UUID.randomUUID(), acc.id, Money(BigDecimal(-50)))
            }
        }

        "cannot withdraw negative amount" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(100)))

            shouldThrow<IllegalArgumentException> {
                service.withdraw(UUID.randomUUID(), acc.id, Money(BigDecimal(-30)))
            }
        }

        "cannot create account with negative balance" {
            val service = createService()
            shouldThrow<IllegalArgumentException> {
                service.createAccount(UUID.randomUUID(), Money(BigDecimal(-10)))
            }
        }

        "concurrent deposits are consistent" {
            val service = createService()
            val acc = service.createAccount(UUID.randomUUID(), Money(BigDecimal(0)))

            runBlocking {
                val jobs =
                    (1..100).map {
                        async {
                            service.deposit(UUID.randomUUID(), acc.id, Money(BigDecimal(1)))
                        }
                    }
                jobs.awaitAll()
            }

            service.getBalance(acc.id) shouldBe Money(BigDecimal(100))
        }

        "failed operation does not block retry" {
            val idempotencyKey = UUID.randomUUID()
            val repo = InMemoryAccountRepository()
            val idempotency = IdempotencyStore()
            val audit = AuditLog()
            val service = BankingService(repo, idempotency, audit)

            // Simulate failure
            shouldThrow<IllegalArgumentException> {
                service.withdraw(idempotencyKey, UUID.randomUUID(), Money(BigDecimal(10)))
            }

            // Retry with same key but valid operation → should succeed
            val acc = service.createAccount(idempotencyKey, Money(BigDecimal(50)))
            service.getBalance(acc.id) shouldBe Money(BigDecimal(50))
        }
    })

fun createService(): BankingService {
    val repo = InMemoryAccountRepository()
    val idempotencyStore = IdempotencyStore()
    val audit = AuditLog()
    return BankingService(repo, idempotencyStore, audit)
}
