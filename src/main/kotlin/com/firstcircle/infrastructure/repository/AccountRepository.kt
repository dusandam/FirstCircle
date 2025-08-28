package org.example.com.firstcircle.infrastructure.repository

import org.example.com.firstcircle.domain.Account
import java.util.UUID

interface AccountRepository {
    fun save(account: Account)

    fun findById(id: UUID): Account?

    fun update(
        id: UUID,
        transformer: (Account) -> Account,
    ): Account
}
