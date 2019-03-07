package com.github.yona168.giraffe.net.messenger.packetprocessor

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class SingleThreadPacketProcessor : FixedThreadPoolPacketProcessor(1)