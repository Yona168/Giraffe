package com.github.yona168.giraffe.net.messenger.packetprocessor

/**
 * A [PacketProcessor] that processes all packets on one thread. This is convinient if packets must be processed
 * in a sequential order. Creating an instance of this class is equivalent to creating a [FixedThreadPoolPacketProcessor]
 * with a thread amount of 1
 */
class SingleThreadPacketProcessor : FixedThreadPoolPacketProcessor(1)