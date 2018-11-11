package com.github.yona168.giraffe.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

fun main(){
    val address= InetSocketAddress("localhost",1234)
    val client=Client(address, TimeUnit.SECONDS, 20){print("Client timed out")}
    client.enable()
    runBlocking { delay(100000) }
}