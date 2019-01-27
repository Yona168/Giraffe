package com.github.yona168.giraffe.net.messenger.packetprocessor

class SingleThreadPacketProcessor(name: String) : FixedThreadPoolPacketProcessor(1, name)