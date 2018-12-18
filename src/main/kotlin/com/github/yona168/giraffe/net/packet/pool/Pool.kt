package com.github.yona168.giraffe.net.packet.pool

import java.util.*

interface Pool<T> {
    val queue: Queue<T>
    fun get(): T {
        val polled = queue.poll()
        return polled ?: createNew()
    }

    fun createNew(): T
    fun release(item: T) = queue.offer(item)
}