package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.packet.packet
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    val address = InetSocketAddress("localhost", 1234)
    val server = GiraffeServer(address)
    server.enable()
    delay(500)
    val client = Client()
    client.connectTo(address, TimeUnit.SECONDS, 15) {}
    client.enable()
    delay(400)
    client.registerPacket(1) {
        println(it.readInt())
    }
    val packet = packet(1) {
        writeInt(54)
    }
   server.sendToAllClients(packet)
    delay(10000)
}