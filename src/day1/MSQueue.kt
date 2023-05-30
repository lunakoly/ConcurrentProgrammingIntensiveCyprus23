package day1

import kotlinx.atomicfu.*

class MSQueue<E> : Queue<E> {
    private val head: AtomicRef<Node<E>>
    private val tail: AtomicRef<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val currentTail = tail.value
            val newNode = Node(element)

            // (1) tail.value can only change when
            // tail.value.next has been updated
            // (2) someone may have updated tail.value.next
            // and already moved tail.value after we have
            // acquired currentTail, but before we have set
            // the new next!
            // (3) ~~yes, and this is may not be a problem:
            // newNode would still be appended to the proper tail,
            // we just wouldn't have moved the tail properly by
            // the end of the operation. However, someone next time
            // would, before they can proceed further~~
            // (4) yes, and this is not a problem:
            // currentTail would still point to the old tail,
            // and its next would not become null, despite the tail
            // has moved.

            if (currentTail.next.compareAndSet(null, newNode)) {
                tail.compareAndSet(currentTail, newNode)
                return
            } else {
                // Someone has added something to next.
                // But they may have not yet moved the tail,
                // so let's try to move it ourselves.

                val next = currentTail.next.value ?: error("We have already checked currentTail.next != null, and it could not have changed")
                tail.compareAndSet(currentTail, next)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val currentHead = head.value
            val nextValue = currentHead.next.value ?: return null

            if (head.compareAndSet(currentHead, nextValue)) {
                return nextValue.element
            }
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = atomic<Node<E>?>(null)
    }
}
