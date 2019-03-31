package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*

/**
 * A [CoroutineScope] that gets a new job when it enables, and cancels it when it disables. This blocks the calling thread.
 */
abstract class ScopedComponent : Component(), CoroutineScope {
    private lateinit var backingJob: Job
    val job: Job
        get() = backingJob

    init {
        onEnable {
            backingJob = Job()
        }
        onDisable {
            runBlocking {
                job.cancelAndJoin()
            }
        }
    }

}
