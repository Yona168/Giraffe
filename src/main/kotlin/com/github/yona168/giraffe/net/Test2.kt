package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.server.GServer
import com.github.yona168.giraffe.net.packet.packet
import javafx.application.Application.launch
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {

        val address = InetSocketAddress("localhost", 1234)
        val server = GServer(address)
        server.enable()
        val client = Client()
        client.connectTo(address, TimeUnit.SECONDS, 15) {}
        client.enable()
        val waiter = ReallyLongWaiterThing()
        var counter=0
        client.registerHandler(1) { packet, client ->
            repeat(20) {
                print(packet.readInt())
            }
            counter++
            if(counter==3){
                waiter.result=true
            }
        }
        client.registerHandler(0) { packet, client ->
            println("UUID= ${packet.readLong()} and ${packet.readLong()}")
        }
        server.registerHandler(1) { packet, client ->
            println("Receiver=Server: ${packet.readString()}")
        }

        val packet = packet(1) {
            repeat(20) {
                writeInt(876)
            }
        }
        repeat(3) {
            server.sendToAllClients(packet)
            println("Wrote packet")
        }
        Thread.sleep(100000)


}


class ReallyLongWaiterThing(var result: Boolean = false)