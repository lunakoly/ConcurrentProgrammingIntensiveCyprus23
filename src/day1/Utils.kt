package day1

import kotlinx.atomicfu.locks.ReentrantLock

inline fun <R> ReentrantLock.withLock(block: () -> R): R {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}
