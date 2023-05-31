package day2

import day1.*
import kotlinx.atomicfu.*
import java.util.concurrent.*

class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = atomic(false) // unlocked initially
    private val tasksForCombiner = atomicArrayOfNulls<Any?>(TASKS_FOR_COMBINER_SIZE)

    private inline fun withLockIfVacant(block: () -> Unit): Boolean {
        val managedToLock = combinerLock.compareAndSet(expect = false, update = true)

        if (managedToLock) {
            try {
                block()
            } finally {
                combinerLock.value = false
            }
        }

        return managedToLock
    }

    private inline fun Boolean.orRun(block: () -> Unit) {
        if (!this) {
            block()
        }
    }

    private fun combine() {
        for (it in 0 until tasksForCombiner.size) {
            when (val task = tasksForCombiner[it].value) {
                null, PROCESSED, is FlatCombiningQueue<*>.DequeResult -> {}
                DEQUE_TASK -> {
                    tasksForCombiner[it].value = DequeResult(queue.removeFirstOrNull())
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    queue.addLast(task as E)
                    tasksForCombiner[it].value = PROCESSED
                }
            }
        }
    }

    override fun enqueue(element: E) {
        // TODO: Make this code thread-safe using the flat-combining technique.
        // TODO: 1.  Try to become a combiner by
        // TODO:     changing `combinerLock` from `false` (unlocked) to `true` (locked).
        // TODO: 2a. On success, apply this operation and help others by traversing
        // TODO:     `tasksForCombiner`, performing the announced operations, and
        // TODO:      updating the corresponding cells to `PROCESSED`.
        // TODO: 2b. If the lock is already acquired, announce this operation in
        // TODO:     `tasksForCombiner` by replacing a random cell state from
        // TODO:      `null` to the element. Wait until either the cell state
        // TODO:      updates to `PROCESSED` (do not forget to clean it in this case),
        // TODO:      or `combinerLock` becomes available to acquire.

        withLockIfVacant {
            queue.addLast(element)
            combine()
        }.orRun {
            val index = acquireRandomCellWith(element)

            while (true) {
                if (tasksForCombiner[index].compareAndSet(PROCESSED, null)) {
                    return
                }

                withLockIfVacant {
                    combine()
                    tasksForCombiner[index].value = null
                    return
                }
            }
        }
    }

    override fun dequeue(): E? {
        // TODO: Make this code thread-safe using the flat-combining technique.
        // TODO: 1.  Try to become a combiner by
        // TODO:     changing `combinerLock` from `false` (unlocked) to `true` (locked).
        // TODO: 2a. On success, apply this operation and help others by traversing
        // TODO:     `tasksForCombiner`, performing the announced operations, and
        // TODO:      updating the corresponding cells to `PROCESSED`.
        // TODO: 2b. If the lock is already acquired, announce this operation in
        // TODO:     `tasksForCombiner` by replacing a random cell state from
        // TODO:      `null` to `DEQUE_TASK`. Wait until either the cell state
        // TODO:      updates to `PROCESSED` (do not forget to clean it in this case),
        // TODO:      or `combinerLock` becomes available to acquire.

        // The DEQUE_TASK result must be returned via the task
        // array, we shouldn't call `removeFirstOrNull()` within
        // the requesting threads. What if two of them wake up
        // at exactly the same moment and attempt to call
        // `removeFirstOrNull()` simultaneously, despite
        // the combiner iterating the array one by one?

        withLockIfVacant {
            return queue.removeFirstOrNull().also {
                combine()
            }
        }.orRun {
            val index = acquireRandomCellWith(DEQUE_TASK)

            while (true) {
                val maybeResult = tasksForCombiner[index].value

                if (maybeResult is FlatCombiningQueue<*>.DequeResult) {
                    tasksForCombiner[index].value = null
                    @Suppress("UNCHECKED_CAST")
                    return maybeResult.value as E
                }

                // The current combiner may have just calculated
                // our result and released the lock

                withLockIfVacant {
                    combine()
                    val definitelyResult = tasksForCombiner[index].value as? FlatCombiningQueue<*>.DequeResult
                        ?: error("Must have been the result after combine()")
                    tasksForCombiner[index].value = null
                    @Suppress("UNCHECKED_CAST")
                    return definitelyResult.value as E
                }
            }
        }

        error("Should not be here")
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.size)

    private fun acquireRandomCellWith(element: Any?): Int {
        while (true) {
            val index = randomCellIndex()

            if (tasksForCombiner[index].compareAndSet(null, element)) {
                return index
            }
        }
    }

    // This wrapper prevents double combine()
    // calls from doing: DEQUE_TASK -> Int -> PROCESSED
    // if the requested thread is not fast enough
    // to collect the dequeue() value.
    inner class DequeResult(val value: E?)
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private val DEQUE_TASK = object : Any() {
    override fun toString() = "DEQUE_TASK"
}

// TODO: Put this token in `tasksForCombiner` when the task is processed.
private val PROCESSED = object : Any() {
    override fun toString() = "PROCESSED"
}