package day4

import day4.DescriptorBase.DescriptorStatus.*
import kotlinx.atomicfu.atomic

class CAS2Descriptor<E>(
    reference1: AtomicRefAccessor<Any?>, private val expected1: E?, private val update1: E?,
    reference2: AtomicRefAccessor<Any?>, private val expected2: E?, private val update2: E?,
    occupy1First: Boolean,
) : DescriptorBase<E>(reference1, reference2, occupy1First) {
    val rawStatus = atomic<Any?>(UNDECIDED)

    val movableStatus = object : AtomicRefAccessor<Any?>() {
        override val value get() = rawStatus.value
        override fun compareAndSet(expected: Any?, update: Any?) = rawStatus.compareAndSet(expected, update)
        override fun toString() = "StatusAR($value)"
    }

    override val status = object : AtomicRefAccessor<DescriptorStatus>() {
        override val value: DescriptorStatus
            get() = when (val it = rawStatus.value) {
                is DCSSDescriptor<*> -> {
                    it.getValueOf(movableStatus) as DescriptorStatus
                }
                else -> it as DescriptorStatus
            }

        override fun compareAndSet(expected: DescriptorStatus, update: DescriptorStatus) =
            rawStatus.compareAndSet(expected, update)
    }

    override fun getRawValueOf(reference: AtomicRefAccessor<Any?>) = when (reference) {
        reference1 -> when (status.value) {
            UNDECIDED, FAILED -> expected1
            SUCCESS -> update1
        }
        reference2 -> when (status.value) {
            UNDECIDED, FAILED -> expected2
            SUCCESS -> update2
        }
        else -> error("The requested reference is not captured by the descriptor")
    }

    private fun canOccupyCellCarefully(reference: AtomicRefAccessor<Any?>, expected: E?): Boolean {
        when (val value = reference.value) {
            this -> return true
            is DescriptorBase<*> -> value.applyOperation()
            // This operation is more complex than CAS, so
            // it's not free to call it straight away:
            // if the descriptor has already been set,
            // then we shouldn't try to call this operation,
            // because something may easily break.
            // If everything is fine, we'll see it during the
            // next iteration.
            expected -> DCSSDescriptor(reference, expected, this, movableStatus, UNDECIDED, true)
                .startOperation()
            else -> status.compareAndSet(UNDECIDED, FAILED)
        }

        return false
    }

    fun startOperation() {
        val (firstReference, firstExpected) = when {
            occupy1First -> reference1 to expected1
            else -> reference2 to expected2
        }

        while (
            status.value != SUCCESS && status.value != FAILED &&
            !canOccupyCell(firstReference, firstExpected)
        ) {
            // Keep trying
        }

        applyOperation()
    }

    override fun applyOperation() {
        val (firstReference, firstExpected, firstUpdate) = when {
            occupy1First -> Triple(reference1, expected1, update1)
            else -> Triple(reference2, expected2, update2)
        }

        val (secondReference, secondExpected, secondUpdate) = when {
            occupy1First -> Triple(reference2, expected2, update2)
            else -> Triple(reference1, expected1, update1)
        }

        while (
            status.value != SUCCESS && status.value != FAILED &&
            !canOccupyCellCarefully(secondReference, secondExpected)
        ) {
            // Keep trying
        }

        // Note that we can't expect that any particular
        // DCSS applyOperation() has finished:
        // the current thread may have read `this` from
        // the reference, and it could have been set
        // by another thread that is processing DCSS
        // and has not yet reverted the status back
        // to UNDECIDED.
        // Reordering the "releases" of the references
        // in DCSS doesn't allow to get rid of this loop.

        while (true) {
            status.compareAndSet(UNDECIDED, SUCCESS)

            when (val value = rawStatus.value) {
                SUCCESS, FAILED -> break
                is DescriptorBase<*> -> value.applyOperation()
            }
        }

        val (firstReset, secondReset) = when (status.value) {
            SUCCESS -> firstUpdate to secondUpdate
            FAILED -> firstExpected to secondExpected
            else -> error("Should not have come this far")
        }

        firstReference.compareAndSet(this, firstReset)
        secondReference.compareAndSet(this, secondReset)
    }
}
