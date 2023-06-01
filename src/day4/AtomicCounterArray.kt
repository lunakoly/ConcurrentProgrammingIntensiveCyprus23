@file:Suppress("DuplicatedCode")

package day4

import day4.AtomicCounterArray.Status.*
import kotlinx.atomicfu.*

// This implementation never stores `null` values.
class AtomicCounterArray(size: Int) {
    private val array = atomicArrayOfNulls<Any>(size)

    init {
        // Fill array with zeros.
        for (i in 0 until size) {
            array[i].value = 0
        }
    }

    // TODO: the cell can store a descriptor.
    fun get(index: Int): Int = when (val value = array[index].value) {
        is IncrementDescriptor -> when (value.status.value) {
            SUCCESS -> value.getValueAfterIncrementFor(index)
            else -> value.getValueBeforeIncrementFor(index)
        }
        else -> value as? Int ?: error("Invalid element type: $value")
    }

    fun inc2(indexA: Int, indexB: Int) {
        require(indexA != indexB) { "The indices should be different" }
        // TODO: This implementation is not linearizable!
        // TODO: Use `IncrementDescriptor` to perform the operation atomically.

        val index1 = if (indexA <= indexB) indexA else indexB
        val index2 = if (indexA <= indexB) indexB else indexA

        while (true) {
            val value1 = get(index1)
            val value2 = get(index2)

            val descriptor = IncrementDescriptor(index1, value1, index2, value2).also {
                it.applyOperation()
            }

            if (descriptor.status.value == SUCCESS) {
                break
            }
        }
    }

    // TODO: Implement the `inc2` operation using this descriptor.
    // TODO: 1) Read the current cell states
    // TODO: 2) Create a new descriptor
    // TODO: 3) Call `applyOperation()` -- it should try to increment the counters atomically.
    // TODO: 4) Check whether the `status` is `SUCCESS` or `FAILED`, restarting in the latter case.
    private inner class IncrementDescriptor(
        val index1: Int, val valueBeforeIncrement1: Int,
        val index2: Int, val valueBeforeIncrement2: Int
    ) {
        val status = atomic(UNDECIDED)

        init {
            require(index1 <= index2) { "index1 <= index2 violated: $index1, $index2" }
        }

        fun getValueBeforeIncrementFor(index: Int) = when (index) {
            index1 -> valueBeforeIncrement1
            index2 -> valueBeforeIncrement2
            else -> error("Index $index is not present in the descriptor residing in this location")
        }

        fun getValueAfterIncrementFor(index: Int) = getValueBeforeIncrementFor(index) + 1

        fun canOccupyCell(index: Int, valueBeforeIncrement: Int): Boolean {
            array[index].compareAndSet(valueBeforeIncrement, this)

            when (val value = array[index].value) {
                this -> return true
                is IncrementDescriptor -> value.applyOperation()
                valueBeforeIncrement -> {} // Didn't manage to do it in time, fuck it then
                else -> status.compareAndSet(UNDECIDED, FAILED)
            }

            return false
        }

        // TODO: Other threads can call this function
        // TODO: to help completing the operation.
        fun applyOperation() {
            // TODO: Use the CAS2 algorithm, installing this descriptor
            // TODO: in `array[index1]` and `array[index2]` cells.

            while (status.value == UNDECIDED) {
                if (!canOccupyCell(index1, valueBeforeIncrement1)) {
                    continue
                }

                if (!canOccupyCell(index2, valueBeforeIncrement2)) {
                    continue
                }

                status.compareAndSet(UNDECIDED, SUCCESS)
            }

            val valueShift = when (status.value) {
                SUCCESS -> 1
                FAILED -> 0
                else -> error("Should have no come this far")
            }

            array[index1].compareAndSet(this, valueBeforeIncrement1 + valueShift)
            array[index2].compareAndSet(this, valueBeforeIncrement2 + valueShift)
        }
    }

    private enum class Status {
        UNDECIDED, FAILED, SUCCESS
    }
}