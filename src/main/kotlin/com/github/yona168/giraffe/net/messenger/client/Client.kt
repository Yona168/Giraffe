package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor

abstract class Client(override val packetProcessor: PacketProcessor) :
    AbstractScopedPacketChannelComponent(packetProcessor), IClient