package com.firstcircle.service

import com.firstcircle.domain.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(classes = [com.firstcircle.FirstCircleApplication::class])
class SpringWiringTest {
    @Autowired
    lateinit var bankingService: BankingService

    @Test
    fun `can deposit and check balance via Spring`() {
        val account = bankingService.createAccount(UUID.randomUUID(), Money(BigDecimal(200)))
        bankingService.deposit(UUID.randomUUID(), account.id, Money(BigDecimal(50)))
        assertEquals(Money(BigDecimal(250)), bankingService.getBalance(account.id))
    }
}
