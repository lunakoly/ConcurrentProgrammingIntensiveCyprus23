package day4

import kotlinx.atomicfu.*

// This implementation never stores `null` values.
class DynamicArray<E: Any> {
    private val core = atomic(Core(capacity = 1)) // Do not change the initial capacity

    private fun allocateNewCoreAfter(currentCore: Core) {
        val newCore = Core(capacity = currentCore.capacity * 2).also { it.size.value = currentCore.capacity }
        currentCore.next.compareAndSet(null, newCore)
        configureNextCore()
    }

    private fun configureNextCore() {
        val currentCore = core.value
        val newCore = currentCore.next.value ?: return

        for (it in 0 until currentCore.size.value) {
            val unwrappedValue = freezeValueAndGetUnwrapped(currentCore, it)
            newCore.array[it].compareAndSet(null, unwrappedValue)
        }

        core.compareAndSet(currentCore, newCore)
    }

    private fun freezeValueAndGetUnwrapped(currentCore: Core, index: Int): E {
        while (true) {
            val value = currentCore.array[index].value
                    ?: error("Should've been a value")

            if (value is DynamicArray<*>.FrozenValue) {
                @Suppress("UNCHECKED_CAST")
                return value.data as E
            }

            @Suppress("UNCHECKED_CAST")
            if (currentCore.array[index].compareAndSet(value, FrozenValue(value as E))) {
                return value
            }

            // Someone has just replaced the value
        }
    }

    /**
     * Adds the specified [element] to the end of this array.
     */
    fun addLast(element: E) {
        // TODO: Implement me!
        // TODO: Yeah, this is a hard task, I know ...

        while (true) {
            val currentCore = core.value
            val curSize = currentCore.size.value

            if (curSize == currentCore.capacity) {
                allocateNewCoreAfter(currentCore)
                continue
            }

            if (currentCore.array[curSize].compareAndSet(null, element)) {
                currentCore.size.compareAndSet(curSize, curSize + 1)
                break
            } else {
                currentCore.size.compareAndSet(curSize, curSize + 1)
            }
        }
    }

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the size of this array.
     */
    fun set(index: Int, element: E) {
        while (true) {
            val curCore = core.value
            val curSize = curCore.size.value
            require(index < curSize) { "index must be lower than the array size" }
            // TODO: check that the cell is not "frozen"

            val value = curCore.array[index].value

            if (value is DynamicArray<*>.FrozenValue) {
                configureNextCore()
                continue
            } else {
                if (curCore.array[index].compareAndSet(value, element)) {
                    break
                }
            }
        }
    }

    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the size of this array.
     */
    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E {
        while (true) {
            val curCore = core.value
            val curSize = curCore.size.value
            require(index < curSize) { "index must be lower than the array size" }
            // TODO: check that the cell is not "frozen",
            // TODO: unwrap the element in this case.

            val value = curCore.array[index].value

            if (value is DynamicArray<*>.FrozenValue) {
                configureNextCore()
                continue
            }

            return value as E
        }
    }

    private class Core(
        val capacity: Int
    ) {
        val array = atomicArrayOfNulls<Any?>(capacity)
        val size = atomic(0)
        val next = atomic<Core?>(null)
    }

    inner class FrozenValue(val data: E) {
        override fun toString() = "Frozen($data)"
    }
}