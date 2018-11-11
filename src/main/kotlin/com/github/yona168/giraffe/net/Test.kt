package com.github.yona168.giraffe.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main(){
    val address=InetSocketAddress("localhost",1234)
    val server=GiraffeServer(address)
    server.enable()
   runBlocking { delay(1000000) }
}