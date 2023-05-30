package day1

import kotlinx.atomicfu.*
import java.util.concurrent.*

class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    // TODO: Try to optimize concurrent push and pop operations,
    // TODO: synchronizing them in an `eliminationArray` cell.
    private val eliminationArray = atomicArrayOfNulls<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    private fun tryPushElimination(element: E): Boolean {
        // TODO: Choose a random cell in `eliminationArray`
        // TODO: and try to install the element there.
        // TODO: Wait `ELIMINATION_WAIT_CYCLES` loop cycles
        // TODO: in hope that a concurrent `pop()` grabs the
        // TODO: element. If so, clean the cell and finish,
        // TODO: returning `true`. Otherwise, move the cell
        // TODO: to the empty state and return `false`.

        val index = randomCellIndex()

        if (!eliminationArray[index].compareAndSet(CELL_STATE_EMPTY, element)) {
            // Progress guarantee
            return false
        }

        for (it in 0 until ELIMINATION_WAIT_CYCLES) {
            if (eliminationArray[index].compareAndSet(CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                return true
            }
        }

        // Someone may have grabbed the element after we
        // stopped checking for CELL_STATE_RETRIEVED

        if (eliminationArray[index].compareAndSet(element, CELL_STATE_EMPTY)) {
            return false
        }

        if (!eliminationArray[index].compareAndSet(CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
            error("Test for element has failed, so this should have not")
        }

        return true
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        val index = randomCellIndex()
        val element = eliminationArray[index].value

        if (element == CELL_STATE_EMPTY || element == CELL_STATE_RETRIEVED) {
            return null
        }

        return if (eliminationArray[index].compareAndSet(element, CELL_STATE_RETRIEVED)) {
            @Suppress("UNCHECKED_CAST")
            element as E?
        } else {
            null
        }

        // TODO: Choose a random cell in `eliminationArray`
        // TODO: and try to retrieve an element from there.
        // TODO: On success, return the element.
        // TODO: Otherwise, if the cell is empty, return `null`.
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.size)

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}