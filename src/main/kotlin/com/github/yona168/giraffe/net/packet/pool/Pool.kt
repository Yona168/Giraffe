package com.github.yona168.giraffe.net.packet.pool

interface Pool<T> {
    val nextItem: T
        get() = get()
    fun get():T
    fun createNew(): T
    fun clearAndRelease(item: T)=release(clear(item))
    fun clear(item:T):T
    fun release(item:T):Boolean

}