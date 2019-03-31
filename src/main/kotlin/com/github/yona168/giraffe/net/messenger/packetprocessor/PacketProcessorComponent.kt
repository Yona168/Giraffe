package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.messenger.ScopedComponent
import com.github.yona168.giraffe.net.onDisable
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*

/**
 * Joins the abstractions for [PacketProcessor] to [ScopedComponent]
 */
abstract class PacketProcessorComponent : PacketProcessor, ScopedComponent()