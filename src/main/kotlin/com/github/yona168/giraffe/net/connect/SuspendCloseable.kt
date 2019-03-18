package com.github.yona168.giraffe.net.connect

interface SuspendCloseable {
    suspend fun close()
}