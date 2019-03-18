package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onEnable
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext


class CoroutineDispatcherPacketProcessor(dispatcher: CoroutineDispatcher) : ScopedPacketProcessor() {
    override lateinit var coroutineContext: CoroutineContext

    init {
        onEnable {
            coroutineContext = job + dispatcher
        }
    }
}

