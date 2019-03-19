package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.messenger.server.GServer
import com.github.yona168.giraffe.net.messenger.server.IServer
import com.gitlab.avelyn.architecture.base.Component

/**
 * The base class for a Client (ie [GClient]. This class's sole purpose is to link to functionalities of
 * [IClient] with [AbstractScopedPacketChannelComponent], which is necessary due to certain functions in [Component]
 * not being declared in interfaces
 */
abstract class Client(packetProcessor: PacketProcessor) :
    AbstractScopedPacketChannelComponent(packetProcessor), IClient