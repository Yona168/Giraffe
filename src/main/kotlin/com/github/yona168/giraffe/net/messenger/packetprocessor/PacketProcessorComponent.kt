package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.onDisable
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Defines a [PacketProcessor] that extends [Component], giving it enable/disable functionality
 * @property[job] the [Job] of this component.
 */
abstract class PacketProcessorComponent : PacketProcessor, Component() {
    val job = Job()

    init {
        onDisable {
            this.coroutineContext.cancel()
        }
    }
}