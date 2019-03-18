package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.gitlab.avelyn.architecture.base.Component

/**
 * The base class for a Server (ie [GServer]). This classe's sole purpose is to link to functionalities of
 * [IServer] with [AbstractScopedPacketChannelComponent], which is necessary due to certain functions in [Component]
 * not being declared in interfaces
 */
abstract class Server(packetProcessor: PacketProcessor) : AbstractScopedPacketChannelComponent(packetProcessor), IServer