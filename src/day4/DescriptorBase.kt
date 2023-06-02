package day4

import day4.DescriptorBase.DescriptorStatus.*
import kotlinx.atomicfu.atomic

abstract class AtomicRefAccessor<E> {
    abstract val value: E

    abstract fun compareAndSet(expected: E, update: E): Boolean
}

abstract class ArrayAtomicRefAccessor(val index: Int) : AtomicRefAccessor<Any?>() {
    override fun equals(other: Any?) = this === other || other is ArrayAtomicRefAccessor && index == other.index
    override fun hashCode() = index
    override fun toString() = "ArrayAR([$index] $value)"
}

class MovableAtomicRef<E>(initialValue: E) : AtomicRefAccessor<E>() {
    private val backingReference = atomic(initialValue)
    override val value get() = backingReference.value
    override fun compareAndSet(expected: E, update: E) = backingReference.compareAndSet(expected, update)
    override fun toString() = "MovableAR($value)"
}

abstract class DescriptorBase<E>(
    protected val reference1: AtomicRefAccessor<Any?>,
    protected val reference2: AtomicRefAccessor<Any?>,
    protected  val occupy1First: Boolean,
) {
    abstract val status: AtomicRefAccessor<DescriptorStatus>

    val isSuccess get() = status.value == SUCCESS

    abstract fun getRawValueOf(reference: AtomicRefAccessor<Any?>): E?

    @Suppress("UNCHECKED_CAST")
    fun getValueOf(reference: AtomicRefAccessor<Any?>): E? =
        when (val value = getRawValueOf(reference)) {
            is DescriptorBase<*> -> value.getValueOf(reference) as E?
            else -> value
        }

    protected fun canOccupyCell(reference: AtomicRefAccessor<Any?>, expected: E?): Boolean {
        reference.compareAndSet(expected, this)

        when (val value = reference.value) {
            this -> return true
            is DescriptorBase<*> -> value.applyOperation()
            // Really? Fuck you
            expected -> {} // Actually, this check is crucial for some reason
            else -> status.compareAndSet(UNDECIDED, FAILED)
        }

        return false
    }

    abstract fun applyOperation()

    enum class DescriptorStatus {
        UNDECIDED, FAILED, SUCCESS
    }
}
