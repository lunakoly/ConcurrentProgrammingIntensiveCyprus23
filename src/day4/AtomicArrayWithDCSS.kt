package day4

import kotlinx.atomicfu.*
import day4.AtomicArrayWithDCSS.Status.*

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
        is AtomicArrayWithDCSS<*>.DCSSDescriptor -> value.getValueOf(index) as E?
        else -> value as E?
    }

    fun cas(index: Int, expected: Any?, update: Any?): Boolean {
        // TODO: the cell can store a descriptor
        while (true) {
            if (array[index].compareAndSet(expected, update)) {
                return true
            }

            when (val value = array[index].value) {
                is AtomicArrayWithDCSS<*>.DCSSDescriptor -> value.applyOperation()
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

        val descriptor = DCSSDescriptor(index1, expected1, update1, index2, expected2)
            .also { it.applyOperation() }

        return descriptor.status.value == SUCCESS
    }

    inner class DCSSDescriptor(
        private val index1: Int, private val expected1: E?, private val update1: E?,
        private val index2: Int, private val expected2: E?,
    ) {
        val status = atomic(UNDECIDED)

        private fun getOldValueOf(index: Int) = when (index) {
            index1 -> expected1
            index2 -> expected2
            else -> error("No such index found in the descriptor: index = $index")
        }

        private fun getNewValueOf(index: Int) = when (index) {
            index1 -> update1
            index2 -> expected2
            else -> error("No such index found in the descriptor: index = $index")
        }

        fun getValueOf(index: Int) = when (status.value) {
            UNDECIDED, FAILED -> getOldValueOf(index)
            SUCCESS -> getNewValueOf(index)
        }

        private fun canOccupyCell(index: Int, expected: E?): Boolean {
            array[index].compareAndSet(expected, this)

            when (val value = array[index].value) {
                this -> return true
                is AtomicArrayWithDCSS<*>.DCSSDescriptor -> value.applyOperation()
                expected -> {} // really?, fuck you
                else -> status.compareAndSet(UNDECIDED, FAILED)
            }

            return false
        }

        fun applyOperation() {
            val (firstIndex, firstExpected) = when {
                index1 <= index2 -> index1 to expected1
                else -> index2 to expected2
            }

            val (secondIndex, secondExpected) = when {
                index1 <= index2 -> index2 to expected2
                else -> index1 to expected1
            }

            while (status.value == UNDECIDED && !canOccupyCell(firstIndex, firstExpected)) {
                // Keep trying
            }

            while (status.value == UNDECIDED && !canOccupyCell(secondIndex, secondExpected)) {
                // Keep trying
            }

            status.compareAndSet(UNDECIDED, SUCCESS)

            val (reset1, reset2) = when (status.value) {
                SUCCESS -> update1 to expected2
                FAILED -> expected1 to expected2
                else -> error("Should not have come this far")
            }

            array[index1].compareAndSet(this, reset1)
            array[index2].compareAndSet(this, reset2)
        }
    }

    enum class Status {
        UNDECIDED, FAILED, SUCCESS
    }
}