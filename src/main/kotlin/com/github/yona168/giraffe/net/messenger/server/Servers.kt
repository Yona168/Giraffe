@file:JvmName("Servers")

package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import java.net.SocketAddress

/**
 * Here for verb-based aspect of Avelyn. Simply calls creates a new GServer with the passed args.
 * Using this also ensures that if any other implementation of [Server] is used as the default, it can be easily replaced
 * without changing any function calls.
 * @param[address] The [SocketAddress] to have the server accept on on enable.
 * @param[packetProcessor] The [PacketProcessor] to use.
 * @return A [Server] that, when enabled, starts accepting clients.
 */
fun accept(address: SocketAddress, packetProcessor: PacketProcessor): Server = GServer(address, packetProcessor)

