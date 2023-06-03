package day4

import kotlinx.atomicfu.*

// This implementation never stores `null` values.
class DynamicArraySimplified<E : Any>(
    private val capacity: Int
) {
    private val array = atomicArrayOfNulls<Any?>(capacity)
    private val size = atomic(0) // never decreases

    fun addLast(element: E): Boolean {
        while (true) {
            val curSize = size.value
            if (curSize == capacity) return false
            // TODO: you need to install the element and
            // TODO: increment the size atomically.
            // TODO: You are NOT allowed to use CAS2,
            // TODO: there is a more efficient and smarter solution!

            if (array[curSize].compareAndSet(null, element)) {
                size.compareAndSet(curSize, curSize + 1)
                break
            } else {
                size.compareAndSet(curSize, curSize + 1)
            }
        }

        return true
    }

    fun set(index: Int, element: E) {
        val curSize = size.value
        require(index < curSize) { "index must be lower than the array size" }
        // As the size never decreases, this update is safe.
        array[index].value = element
    }

    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E {
        val curSize = size.value
        require(index < curSize) { "index must be lower than the array size" }
        // As the size never decreases, this read is safe.
        return array[index].value as E
    }
}