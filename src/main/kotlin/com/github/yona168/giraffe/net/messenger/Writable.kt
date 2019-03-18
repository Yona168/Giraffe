package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.Job

/**
 * Defines an object that can be sent a [SendablePacket]
 */
interface Writable {
    /**
     * Sends a [packet] to the implementation
     *
     * @param packet The packet to send
     */
    fun write(packet: SendablePacket): Job
}