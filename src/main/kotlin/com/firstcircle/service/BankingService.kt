package com.firstcircle.service

import com.firstcircle.domain.Account
import com.firstcircle.domain.Money
import com.firstcircle.infrastructure.AuditLog
import com.firstcircle.infrastructure.IdempotencyStore
import com.firstcircle.infrastructure.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BankingService(
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val idempotency: IdempotencyStore,
    @Autowired private val audit: AuditLog,
) {
    private val logger = LoggerFactory.getLogger(BankingService::class.java)

    fun createAccount(
        operationId: UUID,
        initialDeposit: Money,
        ownerName: String? = null,
        accountNumber: String? = null,
    ): Account =
        withOperationLogging("CREATE_ACCOUNT", operationId, listOf()) {
            idempotency.executeOnce(operationId) {
                val account =
                    Account(balance = initialDeposit, ownerName = ownerName, accountNumber = accountNumber)
                accountRepository.save(account)
                audit.record("CREATE_ACCOUNT", listOf(account.id), initialDeposit)
                account
            }
        }

    fun deposit(
        operationId: UUID,
        accountId: UUID,
        amount: Money,
    ) {
        withOperationLogging("DEPOSIT", operationId, listOf(accountId)) {
            idempotency.executeOnce(operationId) {
                accountRepository.findById(accountId) ?: throw IllegalArgumentException("Account $accountId not found")
                accountRepository.update(accountId) { it.deposit(amount) }
                audit.record("DEPOSIT", listOf(accountId), amount)
            }
        }
    }

    fun withdraw(
        operationId: UUID,
        accountId: UUID,
        amount: Money,
    ) {
        withOperationLogging("WITHDRAW", operationId, listOf(accountId)) {
            idempotency.executeOnce(operationId) {
                accountRepository.findById(accountId) ?: throw IllegalArgumentException("Account $accountId not found")
                accountRepository.update(accountId) { it.withdraw(amount) }
                audit.record("WITHDRAW", listOf(accountId), amount)
            }
        }
    }

    fun transfer(
        operationId: UUID,
        fromAccountId: UUID,
        toAccountId: UUID,
        amount: Money,
    ) {
        withOperationLogging("TRANSFER", operationId, listOf(fromAccountId, toAccountId)) {
            idempotency.executeOnce(operationId) {
                validateAccountsToTransfer(fromAccountId, toAccountId)
                val (firstId, secondId) =
                    listOf(
                        fromAccountId,
                        toAccountId,
                    ).sortedBy { it.toString() } // to avoid deadlocks
                synchronized(firstId.toString().intern()) {
                    synchronized(secondId.toString().intern()) {
                        accountRepository.update(fromAccountId) { it.withdraw(amount) }
                        accountRepository.update(toAccountId) { it.deposit(amount) }
                    }
                }
                audit.record("TRANSFER", listOf(fromAccountId, toAccountId), amount)
            }
        }
    }

    private fun validateAccountsToTransfer(
        fromAccountId: UUID,
        toAccountId: UUID,
    ) {
        if (fromAccountId == toAccountId) throw IllegalArgumentException("Cannot transfer to the same account")
        accountRepository.findById(fromAccountId)
            ?: throw IllegalArgumentException("Account $fromAccountId not found")
        accountRepository.findById(toAccountId)
            ?: throw IllegalArgumentException("Account $toAccountId not found")
    }

    private fun <T> withOperationLogging(
        operation: String,
        operationId: UUID,
        accountIds: List<UUID> = emptyList(),
        block: () -> T,
    ): T {
        val accounts = if (accountIds.isEmpty()) "" else " accounts=${accountIds.joinToString()}"
        logger.info("$operation requested: operationId=$operationId$accounts")
        return try {
            val result = block()
            logger.info("$operation successful: operationId=$operationId$accounts")
            result
        } catch (e: Exception) {
            logger.error("$operation failed: operationId=$operationId$accounts message=${e.message}")
            throw e
        }
    }

    fun getBalance(accountId: UUID): Money =
        accountRepository.findById(accountId)?.balance
            ?: throw IllegalArgumentException("Account $accountId not found")
}
