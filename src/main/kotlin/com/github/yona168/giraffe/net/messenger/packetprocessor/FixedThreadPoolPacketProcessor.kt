package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext

/**
 * A [MappedPacketProcessor] that launches coroutines from a fixed thread pool created with a specified amount of threads.
 * numThreads must NOT exceed the amount defined by:
 *
 *     Runtime.getRuntime().availableProcessors() - 1
 *
 * Enabling will throw [IllegalArgumentException] if the amount of threads exceed the max amount or if the amount is not positive.
 * @param[numThreads] The amount of threads to use in the thread pool
 *
 */
open class FixedThreadPoolPacketProcessor(numThreads: Int) : MappedPacketProcessor() {
    override lateinit var coroutineContext: CoroutineContext
    private lateinit var dispatcher: ExecutorCoroutineDispatcher

    init {
        onEnable {
            val maxThreads = Runtime.getRuntime().availableProcessors() - 1
            if (numThreads > maxThreads) {
                throw IllegalArgumentException("Amount of threads passed in exceeds max thread availability! Your amount: $numThreads. Max amount: $maxThreads")
            } else {
                @Suppress("EXPERIMENTAL_API_USAGE")
                when (numThreads <= 0) {
                    true -> throw IllegalArgumentException("Amount of threads must exceed 0! Your amount: $numThreads")
                    false -> {
                        dispatcher=newFixedThreadPoolContext(numThreads, "Giraffe Pool")
                        coroutineContext=dispatcher+job
                    }
                }
            }
        }
        onDisable {
            dispatcher.close()
        }
    }
}