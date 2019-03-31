@file:JvmName("Clients")

package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.packetprocessor.CustomContextPacketProcessor
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.ByteBufferReceivablePacketPool
import com.github.yona168.giraffe.net.packet.pool.Pool
import java.net.SocketAddress

/**
 * This simply calls [GClient.newClientside] with the passed arguments.
 * This is simply here to provide a layer of abstraction for a default [Client]
 * implementation. If, for some reason, a different implementation of [Client] besides
 *[GClient] is made the default [Client] to use, this function will update with that.
 * @see[GClient.newClientside]
 */
@JvmOverloads
fun connect(
    address: SocketAddress,
    timeoutMillis: Long = 1000,
    packetProcessor: PacketProcessor = CustomContextPacketProcessor.defaultDispatch(),
    pool: Pool<ReceivablePacket> = ByteBufferReceivablePacketPool()
): Client =
    GClient.newClientside(address = address, packetProcessor = packetProcessor, timeoutMillis = timeoutMillis, pool = pool)