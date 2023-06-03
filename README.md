# Intensive Course on Concurrent Programming

## Day 1

### Coarse-grained bank

In [`src/day1/CoarseGrainedBank.kt`](src/day1/CoarseGrainedBank.kt),
make the sequential bank implementation thread-safe.
Please follow the *coarse-grained locking* scheme to make synchronization efficient.
For that, you need to use a single lock to protect all bank operations.

To test your solution, please run:

* `./gradlew test --tests CoarseGrainedBankTest` on Linux or MacOS
* `gradlew test --tests CoarseGrainedBankTest` on Windows

### Fine-grained bank

In [`src/day1/FineGrainedBank.kt`](src/day1/FineGrainedBank.kt),
make the sequential bank implementation thread-safe.
Please follow the *fine-grained locking* scheme to make synchronization efficient.
For that, you need to use per-account locks, thus, ensuring natural parallelism
when accessing different accounts. The `totalAmount()` function should acquire
all the locks to get a consistent snapshot, while `transfer(..)` should acquire
the corresponding account locks.

To test your solution, please run:

* `./gradlew test --tests FineGrainedBankTest` on Linux or MacOS
* `gradlew test --tests FineGrainedBankTest` on Windows

### Treiber stack

In [`src/day1/TreiberStack.kt`](src/day1/TreiberStack.kt),
implement the classic Treiber stack algorithm.

To test your solution, please run:

* `./gradlew test --tests TreiberStackTest` on Linux or MacOS
* `gradlew test --tests TreiberStackTest` on Windows

### Treiber stack with elimination

In [`src/day1/TreiberStackWithElimination.kt`](src/day1/TreiberStackWithElimination.kt),
implement the classic Treiber stack algorithm with the elimination technique.

To test your solution, please run:

* `./gradlew test --tests TreiberStackWithEliminationTest` on Linux or MacOS
* `gradlew test --tests TreiberStackWithEliminationTest` on Windows

### Michael-Scott queue

In [`src/day1/MSQueue.kt`](src/day1/MSQueue.kt),
implement the Michael-Scott queue algorithm.
You might also be interested in the [original paper](http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf).

To test your solution, please run:

* `./gradlew test --tests MSQueueTest` on Linux or MacOS
* `gradlew test --tests MSQueueTest` on Windows


## Day 2

### FAA-based queue: simplified

In [`src/day2/FAABasedQueueSimplified.kt`](src/day2/FAABasedQueueSimplified.kt),
implement a concurrent queue that leverages the `Fetch-and-Add` synchronization primitive.
The high-level design of this queue bases on a conceptually infinite array for storing elements and manipulates
`enqIdx` and `deqIdx` counters, which reference the next working cells in the infinite array for `enqueue(..)`
and `dequeue()` operations.

In this task, use a big plain array as the infinite array implementation.

To test your solution, please run:

* `./gradlew test --tests FAABasedQueueSimplifiedTest` on Linux or MacOS
* `gradlew test --tests FAABasedQueueSimplifiedTest` on Windows

### FAA-based queue

In [`src/day2/FAABasedQueue.kt`](src/day2/FAABasedQueue.kt),
implement a concurrent queue that leverages the `Fetch-and-Add` synchronization primitive.
The high-level design of this queue bases on a conceptually infinite array for storing elements and manipulates
`enqIdx` and `deqIdx` counters, which reference the next working cells in the infinite array for `enqueue(..)`
and `dequeue()` operations.

The infinite array implementation should be simulated via a linked list of
fixed-size segments. The overall algorithm should be obstruction-free or lock-free.

To test your solution, please run:

* `./gradlew test --tests FAABasedQueueTest` on Linux or MacOS
* `gradlew test --tests FAABasedQueueTest` on Windows

### Flat-combining queue

In [`src/day2/FlatCombiningQueue.kt`](src/day2/FlatCombiningQueue.kt), implement a concurrent queue via the
_flat-combining_ technique,
using a sequential queue under the hood. You might be interested in the corresponding
[academic paper]((https://dl.acm.org/doi/pdf/10.1145/1810479.1810540?casa_token=Yo13gxOeFhwAAAAA:qS33gvUFNhI4t_2ioHnZz0egK8PFq0Mg7MT0ma1-26aeQYKk7aZBzEHEY6iFMiu-GEmzsBMuSibDkg))

To test your solution, please run:

* `./gradlew test --tests FlatCombiningQueueTest` on Linux or MacOS
* `gradlew test --tests FlatCombiningQueueTest` on Windows


## Day 3

### Linear-time removals in Michael-Scott queue

In [`src/day3/MSQueueWithLinearTimeRemove.kt`](src/day3/MSQueueWithLinearTimeRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should find the first node that contains the specified element
in linear time and then remove this node also in linear time.

To test your solution, please run:

* `./gradlew test --tests MSQueueWithLinearTimeRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithLinearTimeRemoveTest` on Windows

### Constant-time removals in Michael-Scott queue

In [`src/day3/MSQueueWithLinearTimeRemove.kt`](src/day3/MSQueueWithLinearTimeRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should find the first node that contains the specified element
in linear time, but remove this node in _constant_ time.

* `./gradlew test --tests MSQueueWithConstantTimeRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithConstantTimeRemoveTest` on Windows

### Array of Atomic Counters

In [`src/day4/AtomicCounterArray.kt`](src/day4/AtomicCounterArray.kt),
implement the `inc2(..)` function that atomically increments two counters.
using the CAS2 algorithm. In this data structure, all successful updates
install unique values in the array cells.
This property enables simpler CAS2 implementation.

To test your solution, please run:

* `./gradlew test --tests AtomicCounterArrayTest` on Linux or MacOS
* `gradlew test --tests AtomicCounterArrayTest` on Windows

### Double-Compare-Single-Set

In [`src/day4/AtomicArrayWithDCSS.kt`](src/day4/AtomicArrayWithDCSS.kt),
implement the `dcss(..)` operation. Similarly to CAS2, it requires
allocating a descriptor and installing it in the updating memory location.
We need the `dcss(..)` operation for the next task, to resolve the ABA-problem
in the CAS2 algorithm.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithDCSSTest` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithDCSSTest` on Windows

### CAS2

In [`src/day4/AtomicArrayWithCAS2.kt`](src/day4/AtomicArrayWithCAS2.kt),
implement the `cas2(..)` operation. Unlike in the array of atomic counters,
which values always increase, now updates are no longer unique.
This can lead to the ABA problem. To solve it, please use
the Double-Compare-Single-Set operation when installing CAS2 descriptors.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2Test` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2Test` on Windows


## Day 4

### Dynamic array of limited capacity

In [`src/day4/DynamicArraySimplified.kt`](src/day4/DynamicArraySimplified.kt),
implement a lock-free dynamic array of limited capacity.
This is reminiscence of `vector` in C++ and `ArrayList` in Java,
with the only difference that `addLast(element)` files and returns `false`
if it would exceed the specified capacity.

To test your solution, please run:

* `./gradlew test --tests DynamicArraySimplifiedTest` on Linux or MacOS
* `gradlew test --tests DynamicArraySimplifiedTest` on Windows

### Dynamic array

In [`src/day4/DynamicArray.kt`](src/day4/DynamicArray.kt),
implement a lock-free dynamic array of unlimited capacity.
To implement the resizing procedure, use the technique under
the hood of the open addressing hash table.

You do not need to implement an efficient cooperative elements
transition; each thread is eligible to go over the whole array
to guarantee that all elements are successfully moved to a new
version of the array. While this strategy is inefficient in practice,
it is good enough to learning new techniques.
Implementing efficient cooperative elements transition may take weeks.

To test your solution, please run:

* `./gradlew test --tests DynamicArrayTest` on Linux or MacOS
* `gradlew test --tests DynamicArrayTest` on Windows

### Open-addressing hash table

In [`src/day4/IntIntHashMap.kt`](src/day4/IntIntHashMap.kt),
make the sequential open-addressing hash table linearizable and lock-free.

1. The general code design should stay the same.
2. Do not change the initial capacity (the `INITIAL_CAPACITY` field).
3. The provided `IntIntHashMap` implementation always doubles the table size, even if the table is full of removed
   elements. You do not need to fix this.
4. In the class `IntIntHashMap.Core`, add a new `next: AtomicRef<Core>` field, which references the next version of the
   table.
5. `IntIntHashMap` supports only positive keys and values strictly lower `Int.MAX_VALUE`. Use negative numbers
   and `Int.MAX_VALUE` for the algorithm needs.
6. You do not need to implement cooperative rehashing.

You might also be interested in the corresponding [academic paper](https://arxiv.org/pdf/cs/0303011.pdf).

To test your solution, please run:

* `./gradlew test --tests IntIntHashMapTest` on Linux or MacOS
* `gradlew test --tests IntIntHashMapTest` on Windows
