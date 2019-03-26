package com.github.yona168.giraffe.net.packet.pool

/**
 * A supplier of objects that reuses passed old ones. This allows for less instances and thus more efficiency.
 */
interface Pool<T> {
    /**
     * returns the next item in the pool. Equivalent to calling [get].
     */
    val nextItem: T
        get() = get()

    /**
     * @return the next item in the pool.
     */
    fun get(): T

    /**
     * Makes the passed [item] available for reuse, and thus able to be passed to the pool.
     * @param[item] the item to clear.
     * @return the item.
     */
    fun clear(item: T): T

    /**
     * Releases the passed [item] back into the pool WITHOUT clearing it first.
     * @param[item] the item to release.
     * @return true if successfully released.
     */
    fun release(item: T): Boolean

    /**
     *Calls clear on the passed [item] and THEN releases it.
     * @param[item] the item to clear and release.
     * @return true if successfully released.
     */
    fun clearAndRelease(item: T) = release(clear(item))

}