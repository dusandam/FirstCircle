package com.firstcircle.domain

import java.math.BigDecimal

@JvmInline
value class Money(
    private val amount: BigDecimal,
) {
    init {
        require(amount.scale() <= 2) { "Max 2 decimals" }
        require(amount >= BigDecimal.ZERO) { "Negative not allowed" }
    }

    operator fun plus(sum: Money) = Money(this.amount + sum.amount)

    operator fun minus(sum: Money): Money {
        require(this.amount >= sum.amount)
        return Money(this.amount - sum.amount)
    }
}
