package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.FixedThreadPoolPacketProcessor
import com.github.yona168.giraffe.net.messenger.packetprocessor.SingleThreadPacketProcessor
import com.github.yona168.giraffe.net.messenger.server.GServer
import com.github.yona168.giraffe.net.packet.packet
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit


fun main() {

    val address = InetSocketAddress("localhost", 1234)

    val server = GServer(address)
    server.enable()
    val packetProcessor = SingleThreadPacketProcessor("Test Processor")
    val threadPoolPacketProcessor = FixedThreadPoolPacketProcessor(3, "Testing")
    val client = Client(threadPoolPacketProcessor)
    client.connectTo(address, TimeUnit.SECONDS, 15) {}
    client.enable()
    val waiter = ReallyLongWaiterThing()
    var counter = 0
    client.registerHandler(1) { packet, client ->
        repeat(20) {
            println("$it:${packet.readInt()}")
        }
        counter++
        if (counter == 30) {
            waiter.result = true
        }
    }
    client.registerHandler(0) { packet, client ->
        println("UUID=${packet.readLong()} and ${packet.readLong()}")
    }
    server.registerHandler(1) { packet, client ->
        println("Receiver=Server: ${packet.readString()}")
    }

    val packet = packet(1) {
        println("-------------------")
        repeat(20) {
            writeInt(876)
        }
        println("--------------------")
    }
    runBlocking { delay(1000) }
    repeat(30) {
        server.sendToAllClients(packet)
        println("Wrote packet")
    }
    runBlocking {
        while (true) {
            yield()
        }
    }


}
