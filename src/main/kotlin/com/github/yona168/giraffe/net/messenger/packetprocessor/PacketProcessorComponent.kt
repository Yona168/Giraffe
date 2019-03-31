package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Defines a [PacketProcessor] that extends [Component]
 *
 * @property[job] the [Job] of this component.
 * On disable, [PacketProcessor.coroutineContext] is cancelled
 */
abstract class PacketProcessorComponent : PacketProcessor, Component() {
    val job = Job()

    init {
        onDisable {
            this.coroutineContext.cancel()
        }
    }

}