package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onEnable
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext

open class FixedThreadPoolPacketProcessor(numThreads: Int, name: String) : ScopedPacketProcessor() {
    override lateinit var coroutineContext: CoroutineContext

    init {
        onEnable {
            val maxThreads = Runtime.getRuntime().availableProcessors() - 1
            coroutineContext = job + when (numThreads > maxThreads) {
                true -> throw IllegalArgumentException("Amount of threads passed in exceeds max thread availability! Your amount: $numThreads. Max amount: $maxThreads")
                false -> {
                    when (numThreads <= 0) {
                        true -> throw IllegalArgumentException("Amount of threads must exceed 0! Your amount: $numThreads")

                        false -> newFixedThreadPoolContext(numThreads, name)
                    }
                }
            }
        }
    }
}