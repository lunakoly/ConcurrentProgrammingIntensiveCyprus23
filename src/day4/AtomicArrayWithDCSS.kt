package day4

import kotlinx.atomicfu.*

// This implementation never stores `null` values.
class AtomicArrayWithDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = atomicArrayOfNulls<Any>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i].value = initialValue
        }
    }

    // TODO: the cell can store a descriptor
    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E? = when (val value = array[index].value) {
        is DCSSDescriptor<*> -> value.getValueOf(accessor(index)) as E?
        else -> value as E?
    }

    fun cas(index: Int, expected: Any?, update: Any?): Boolean {
        // TODO: the cell can store a descriptor
        while (true) {
            if (array[index].compareAndSet(expected, update)) {
                return true
            }

            when (val value = array[index].value) {
                is DCSSDescriptor<*> -> value.applyOperation()
                expected -> {}
                else -> return false
            }
        }
    }

    fun dcss(
        index1: Int, expected1: E?, update1: E?,
        index2: Int, expected2: E?
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // TODO This implementation is not linearizable!
        // TODO Store a DCSS descriptor in array[index1].

        val descriptor = DCSSDescriptor(
            accessor(index1), expected1, update1,
            accessor(index2), expected2,
            occupy1First = index1 <= index2,
        )

        return descriptor.also { it.startOperation() }.isSuccess
    }

    private fun accessor(index: Int) = object : ArrayAtomicRefAccessor(index) {
        override val value get() = array[index].value
        override fun compareAndSet(expected: Any?, update: Any?) = array[index].compareAndSet(expected, update)
    }
}