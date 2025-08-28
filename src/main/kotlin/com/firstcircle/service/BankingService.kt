package org.example.com.firstcircle.service

import org.example.com.firstcircle.domain.Account
import org.example.com.firstcircle.domain.Money
import org.example.com.firstcircle.infrastructure.AuditLog
import org.example.com.firstcircle.infrastructure.IdempotencyStore
import org.example.com.firstcircle.infrastructure.repository.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BankingService(
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val idempotency: IdempotencyStore,
    @Autowired private val audit: AuditLog,
) {
    fun createAccount(
        operationId: UUID,
        initialDeposit: Money,
        ownerName: String? = null,
        accountNumber: String? = null,
    ): Account =
        idempotency.executeOnce(operationId) {
            val account = Account(balance = initialDeposit, ownerName = ownerName, accountNumber = accountNumber)
            accountRepository.save(account)
            audit.record("CREATE_ACCOUNT", listOf(account.id), initialDeposit)
            account
        }

    fun deposit(
        operationId: UUID,
        accountId: UUID,
        amount: Money,
    ) {
        idempotency.executeOnce(operationId) {
            accountRepository.findById(accountId) ?: throw IllegalArgumentException("Account $accountId not found")
            accountRepository.update(accountId) { it.deposit(amount) }
            audit.record("DEPOSIT", listOf(accountId), amount)
        }
    }

    fun withdraw(
        operationId: UUID,
        accountId: UUID,
        amount: Money,
    ) {
        idempotency.executeOnce(operationId) {
            accountRepository.findById(accountId) ?: throw IllegalArgumentException("Account $accountId not found")
            audit.record("WITHDRAW", listOf(accountId), amount)
            accountRepository.update(accountId) { it.withdraw(amount) }
        }
    }

    fun transfer(
        operationId: UUID,
        fromAccountId: UUID,
        toAccountId: UUID,
        amount: Money,
    ) {
        idempotency.executeOnce(operationId) {
            if (fromAccountId == toAccountId) throw IllegalArgumentException("Cannot transfer to the same account")
            accountRepository.findById(fromAccountId) ?: throw IllegalArgumentException("Account $fromAccountId not found")
            accountRepository.findById(toAccountId) ?: throw IllegalArgumentException("Account $toAccountId not found")
            val (firstId, secondId) = listOf(fromAccountId, toAccountId).sortedBy { it.toString() } // to avoid deadlocks
            synchronized(firstId.toString().intern()) {
                synchronized(secondId.toString().intern()) {
                    accountRepository.update(fromAccountId) { it.withdraw(amount) }
                    accountRepository.update(toAccountId) { it.deposit(amount) }
                }
            }
            audit.record("TRANSFER", listOf(fromAccountId, toAccountId), amount)
        }
    }

    fun getBalance(accountId: UUID): Money =
        accountRepository.findById(accountId)?.balance
            ?: throw IllegalArgumentException("Account $accountId not found")
}
