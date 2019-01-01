package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.server.GServer
import com.github.yona168.giraffe.net.packet.packet
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    val address = InetSocketAddress("localhost", 1234)
    val server = GServer(address)
    server.enable()
    delay(500)
    val client = Client()
    client.connectTo(address, TimeUnit.SECONDS, 15) {}
    client.enable()
    delay(400)
    client.registerHandler(1) { packet, client ->
        println("Receiver=Client: ${packet.readString()}")
    }
    server.registerHandler(1) { packet, client ->
        println("Receiver=Server: ${packet.readString()}")
    }

    val packet = packet(1) {
        writeString("Hello!!!")
    }
   repeat(200) {
       client.write(packet)
   }

    delay(100000)
}