package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor

abstract class Server(packetProcessor: PacketProcessor) : AbstractScopedPacketChannelComponent(packetProcessor), IServer