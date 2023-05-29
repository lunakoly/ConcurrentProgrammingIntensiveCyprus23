package day1

import kotlinx.atomicfu.*

class TreiberStack<E> : Stack<E> {
    // Initially, the stack is empty.
    private val top = atomic<Node<E>?>(null)

    override fun push(element: E) {
        // TODO: Make me linearizable!
        // TODO: Update `top` via Compare-and-Set,
        // TODO: restarting the operation on CAS failure.
        while (true) {
            val curTop = top.value
            // (1) top may have changed
            // (2) someone may have updated top after we acquired top.value,
            // so the new top now has a new top.value, so we shouldn't modify
            // the old top.value
            // (3) top.value should always seem null to any CAS
            // (4) the saved curTop, thought, will have changed to a non-null value
            val newTop = Node(element, curTop)
            if (top.compareAndSet(curTop, newTop)) {
                return
            }
        }
    }

    override fun pop(): E? {
        // TODO: Make me linearizable!
        // TODO: Update `top` via Compare-and-Set,
        // TODO: restarting the operation on CAS failure.
        while (true) {
            val curTop = top.value ?: return null
            val newTop = curTop.next.value
            if (top.compareAndSet(curTop, newTop)) {
                return curTop.element
            }
        }
    }

    private class Node<E>(
        val element: E,
        next: Node<E>?
    ) {
        val next = atomic(next)
    }
}