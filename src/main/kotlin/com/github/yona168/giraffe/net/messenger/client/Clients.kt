@file:JvmName("Clients")

package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import java.net.SocketAddress

/**
 * Here for verb-based aspect of Avelyn. Simply calls  [GClient.newClientside] with the passed args.
 * Using this also ensures that if any other implementation of [Client] is used as the default, it can be easily replaced
 * without changing any function calls.
 * @param[address] The [SocketAddress] to connect to on enable.
 * @param[packetProcessor] The [PacketProcessor] to use.
 * @param[timeoutMillis] How long the client will wait to finish connecting before giving up (in milliseconds).
 * @return A [Client] that, when enabled, attempts to connect to a server specified by the [address]
 */
fun connect(address: SocketAddress, packetProcessor: PacketProcessor, timeoutMillis: Long): Client =
    GClient.newClientside(address = address, packetProcessor = packetProcessor, timeoutMillis = timeoutMillis)