package com.firstcircle.infrastructure.repository

import com.firstcircle.domain.Account
import com.firstcircle.infrastructure.repository.AccountRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryAccountRepository : AccountRepository {
    private val accounts = ConcurrentHashMap<UUID, Account>()

    override fun save(account: Account) {
        accounts[account.id] = account
    }

    override fun findById(id: UUID): Account? = accounts[id]

    override fun update(
        id: UUID,
        transformer: (Account) -> Account,
    ): Account =
        accounts.compute(id) { _, account ->
            account ?: throw IllegalArgumentException("Account id: $id not found")
            transformer(account)
        }!!
}
