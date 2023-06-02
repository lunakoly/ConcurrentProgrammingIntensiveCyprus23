package day4

import day4.DescriptorBase.DescriptorStatus.*

class DCSSDescriptor<E>(
    reference1: AtomicRefAccessor<Any?>, private val expected1: E?, private val update1: E?,
    reference2: AtomicRefAccessor<Any?>, private val expected2: E?,
    occupy1First: Boolean,
) : DescriptorBase<E>(reference1, reference2, occupy1First) {
    override val status = MovableAtomicRef(UNDECIDED)

    override fun getRawValueOf(reference: AtomicRefAccessor<Any?>) = when (reference) {
        reference1 -> when (status.value) {
            UNDECIDED, FAILED -> expected1
            SUCCESS -> update1
        }
        reference2 -> expected2
        else -> error("The requested reference is not captured by the descriptor")
    }

    fun startOperation() {
        val (firstReference, firstExpected) = when {
            occupy1First -> reference1 to expected1
            else -> reference2 to expected2
        }

        while (status.value == UNDECIDED && !canOccupyCell(firstReference, firstExpected)) {
            // Keep trying
        }

        applyOperation()
    }

    override fun applyOperation() {
        val (firstReference, firstExpected, firstUpdate) = when {
            occupy1First -> Triple(reference1, expected1, update1)
            else -> Triple(reference2, expected2, expected2)
        }

        val (secondReference, secondExpected, secondUpdate) = when {
            occupy1First -> Triple(reference2, expected2, expected2)
            else -> Triple(reference1, expected1, update1)
        }

        // If we don't lock the second reference with the
        // current descriptor, then someone may be able
        // to mutate it faster than we manage to assign
        // the update1 to the first one (logically).

        while (status.value == UNDECIDED && !canOccupyCell(secondReference, secondExpected)) {
            // Wait
        }

        status.compareAndSet(UNDECIDED, SUCCESS)

        val (firstReset, secondReset) = when (status.value) {
            SUCCESS -> firstUpdate to secondUpdate
            FAILED -> firstExpected to secondExpected
            else -> error("Should not have come this far")
        }

        firstReference.compareAndSet(this, firstReset)
        secondReference.compareAndSet(this, secondReset)
    }
}
