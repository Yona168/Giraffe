package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onEnable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * A [MappedPacketProcessor] that handles packets on a custom [CoroutineContext].
 * @param[context] the [CoroutineContext] to use to handle packets.
 */
class CustomContextPacketProcessor(context: CoroutineContext) : MappedPacketProcessor() {
    override lateinit var coroutineContext: CoroutineContext

    init {
        onEnable {
            coroutineContext = job + context
        }
    }

    companion object {
        /**
         * Convenience function to create a [CustomContextPacketProcessor] with a [coroutineContext]
         * of [Dispatchers.Default]
         */
        @JvmStatic
        fun defaultDispatch() = CustomContextPacketProcessor(Dispatchers.Default)
    }
}

