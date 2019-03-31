@file:JvmName("Servers")

package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.messenger.packetprocessor.SingleThreadPacketProcessor
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.ByteBufferReceivablePacketPool
import com.github.yona168.giraffe.net.packet.pool.Pool
import java.net.SocketAddress

/**
 * Creates a new [GServer] with the passed arguments.
 * If another implementation of [Server] is made default besides [GServer], this function will change to reflect that.
 * @param[address] The [SocketAddress] to have the server accept on on enable.
 * @param[packetProcessor] The [PacketProcessor] to use, defaulting to [SingleThreadPacketProcessor].
 * @param[pool] The [Pool] to get empty packets from.
 * @return A [Server] that, when enabled, starts accepting clients.
 */
@JvmOverloads
fun accept(
    address: SocketAddress,
    packetProcessor: PacketProcessor = SingleThreadPacketProcessor(),
    pool: Pool<ReceivablePacket> = ByteBufferReceivablePacketPool()
): Server = GServer(address, packetProcessor, pool)

