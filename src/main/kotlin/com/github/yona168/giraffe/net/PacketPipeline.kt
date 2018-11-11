package com.github.yona168.giraffe.net

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class PacketPipeline(){
    val channel=Channel<Int>()

}