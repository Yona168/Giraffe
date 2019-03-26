package com.github.yona168.giraffe.net.packet.pool

/**
 * A pool that can supply new instances if none are available.
 */
interface FluidPool<T> : Pool<T> {
    /**
     * Creates a new instance.
     * @return the created instance.
     */
    fun createNew(): T
}