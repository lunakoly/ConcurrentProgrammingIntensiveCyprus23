package day2

import day1.*
import kotlinx.atomicfu.*

class InfiniteArrayOfNulls<E> {
    private val segmentHead: AtomicRef<Segment>
    private val segmentTail: AtomicRef<Segment>

    init {
        val firstSegment = Segment(0)
        segmentHead = atomic(firstSegment)
        segmentTail = atomic(firstSegment)
    }

    fun getSegmentByIndex(index: Int): Pair<Segment, Int> {
        val segmentIndex = index / SEGMENT_SIZE
        val localIndex = index % SEGMENT_SIZE
        var current = segmentHead.value

        while (current.index < segmentIndex) {
            val maybeNextSegment = Segment(current.index + 1)

            current = if (current.next.compareAndSet(null, maybeNextSegment)) {
                segmentTail.value = maybeNextSegment
                maybeNextSegment
            } else {
                current.next.value ?: error("CAS == null failed, but no next segment found")
            }
        }

        return current to localIndex
    }

    fun moveHeadToNextSegmentAfter(segment: Segment) {
        val nextSegment = segment.next.value ?: return
        segmentHead.compareAndSet(segment, nextSegment)
    }

    inner class Segment(
        val index: Int,
    ) {
        val data = atomicArrayOfNulls<E>(SEGMENT_SIZE)
        val next = atomic<Segment?>(null)
    }
}

// TODO: Copy the code from `FAABasedQueueSimplified`
// TODO: and implement the infinite array on a linked list
// TODO: of fixed-size segments.
class FAABasedQueue<E> : Queue<E> {
    private val infiniteArray = InfiniteArrayOfNulls<Any>()

    private val enqIdx = atomic(0)
    private val deqIdx = atomic(0)

    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()
            val (segment, index) = infiniteArray.getSegmentByIndex(i)

            if (segment.data[index].compareAndSet(null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (deqIdx.value >= enqIdx.value) return null
            val i = deqIdx.getAndIncrement()
            val (segment, index) = infiniteArray.getSegmentByIndex(i)

            if (segment.data[index].compareAndSet(null, POISONED)) {
                if (index == SEGMENT_SIZE - 1) {
                    infiniteArray.moveHeadToNextSegmentAfter(segment)
                }

                continue
            }

            return segment.data[index].value as E
        }
    }
}

private val POISONED = Any()

private const val SEGMENT_SIZE = 8
