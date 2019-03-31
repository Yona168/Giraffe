package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * A [CoroutineScope] that gets a new job when it enables, and cancels it when it disables.
 */
abstract class ScopedComponent : Component(), CoroutineScope {
    private lateinit var backingJob: Job
    /**
     * If all resources have been closed/cancelled
     */
    open val isCancelled:Boolean
        get()=job.isCancelled
    val job: Job
        get() = backingJob

    init {
        onEnable {
            backingJob = Job()
        }
        onDisable {
            job.cancel()
        }
    }

}
