package com.github.yona168.giraffe.net.packet.pool

import java.util.*

interface Pool<T> {
    val nextItem: T
        get() = get()
    fun get():T
    fun createNew(): T
    fun release(item: T):Boolean

}