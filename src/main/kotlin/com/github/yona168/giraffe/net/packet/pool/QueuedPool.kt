package com.github.yona168.giraffe.net.packet.pool

import java.util.*

/**
 * An implementation of [FluidPool] that uses a [Deque] to store objects.
 */
abstract class QueuedPool<T> : FluidPool<T> {
    private val pool: Deque<T> = ArrayDeque<T>()
    /**
     * @return the next item in the internal queue if one exists. Otherwise, the result of [createNew] is returned.
     */
    override fun get()=pool.poll()?:createNew()

    override fun release(item: T)=pool.offer(item)
}