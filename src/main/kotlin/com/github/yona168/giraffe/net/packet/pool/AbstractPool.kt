package com.github.yona168.giraffe.net.packet.pool

import java.util.*

abstract class AbstractPool<T> : Pool<T> {
    private val pool: Deque<T> = ArrayDeque<T>()
    override fun get()=pool.poll()?:createNew()
    override fun release(item: T)=pool.offer(item)
}