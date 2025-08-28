package com.firstcircle.infrastructure

import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class IdempotencyStore {
    private val processed = ConcurrentHashMap<UUID, Any>()

    fun <T : Any> executeOnce(
        key: UUID,
        block: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return processed.computeIfAbsent(key) { block() } as T
    }
}
