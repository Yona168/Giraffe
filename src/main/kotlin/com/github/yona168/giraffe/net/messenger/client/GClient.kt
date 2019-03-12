package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.packet.HANDSHAKE_SUB_IDENTIFIER
import com.github.yona168.giraffe.net.packet.INTERNAL_OPCODE
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * The basic implementation of
 */
@ExperimentalCoroutinesApi
class GClient @JvmOverloads constructor(
    private val address: SocketAddress,
    packetProcessor: ScopedPacketProcessor,
    override val socketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) : AbstractScopedPacketChannelComponent(packetProcessor),
    Client {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val inbox = ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE)
    private val controller = Mutex()

    private object ReadCompletionHandler : ContinuationCompletionHandler<Int>()
    private object WriteCompletionHandler : ContinuationCompletionHandler<Int>()

    private val onConnectListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private val onDisconnectListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private val onPacketReceiveListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private val onHandshakeListeners: MutableSet<PacketHandlerFunction> = mutableSetOf()
    private var backingSessionUUID: UUID? = null
    val sessionUUID: UUID?
        get() = backingSessionUUID

    init {
        onEnable {
            packetProcessor.reigster(INTERNAL_OPCODE) { packet, client ->
                if (packet.readByte() == HANDSHAKE_SUB_IDENTIFIER) {
                    onHandshakeListeners.forEach { it(packet, this) }
                }
            }
            onHandshake { packet, _ ->
                backingSessionUUID = UUID(packet.readLong(), packet.readLong())
            }
            loopRead()
        }
    }

    private fun connect(
        address: SocketAddress,
        unit: TimeUnit,
        timeout: Long,
        onTimeoutFunction: (GClient) -> Unit
    ) {
        val connectionResult = runCatching<GClient> {
            socketChannel.connect(address).get(timeout, unit)
            this
        }
        connectionResult.onSuccess { onConnectListeners.forEach { it(this) } }
        connectionResult.onFailure { exc ->
            when (exc) {
                is TimeoutException -> {
                    onTimeoutFunction(this)
                }
                else -> {
                    throw exc
                }
            }
        }
    }

    fun connect(address: SocketAddress): GClient {
        connect(address, TimeUnit.DAYS, 1) {}
        return this
    }

    override fun write(packet: SendablePacket) {
        launch(coroutineContext) {
            controller.withLock {
                val buf = packet.build()
                while (buf.hasRemaining()) {
                    aWrite(buf)
                }
            }
        }
    }

    override fun onConnect(func: (Client) -> Unit) = onConnectListeners.add(func)
    override fun onConnect(func: () -> Unit) = onConnect { _: Client -> func() }
    /*
    override fun onConnect(func: Runnable) = onConnect { _: Client -> func.run() }
    override fun onConnect(func: Consumer<Client>) = onConnect { client -> func.accept(client) }
    */

    override fun onDisconnect(func: (Client) -> Unit) = onDisconnectListeners.add(func)
    override fun onDisconnect(func: () -> Unit) = onDisconnect { _: Client -> func() }
    override fun onDisconnect(func: Runnable) = onDisconnect { _: Client -> func.run() }
    override fun onDisconnect(func: Consumer<Client>) = onDisconnect { client -> func.accept(client) }

    override fun onPacketReceive(func: (Client) -> Unit) = onPacketReceiveListeners.add(func)
    override fun onPacketReceive(func: () -> Unit) = onPacketReceive { _: Client -> func() }
    override fun onPacketReceive(func: Runnable) = onPacketReceive { _: Client -> func.run() }
    override fun onPacketReceive(func: Consumer<Client>) = onPacketReceive { client -> func.accept(client) }

    fun onHandshake(func: PacketHandlerFunction): Boolean {
        return onHandshakeListeners.add(func)
    }

    fun onHandshake(func: BiConsumer<ReceivablePacket, Writable>) =
        onHandshake { packet, client -> func.accept(packet, client) }

    private suspend fun read(): Int {
        val read = withContext(coroutineContext) {
            read(inbox)
        }
        return when (read) {
            -1 -> {
                this@GClient.disable()
                0
            }
            else -> read
        }
    }

    private suspend fun read(buf: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
        socketChannel.read(buf, cont, ReadCompletionHandler)
    }

    private suspend fun aWrite(buf: ByteBuffer): Int = suspendCancellableCoroutine {
        socketChannel.write(buf, it, WriteCompletionHandler)
    }


    private fun loopRead() = launch(coroutineContext + Dispatchers.Default) {
        var opcode: Opcode? = null
        var size = OPCODE_AND_SIZE_BYTE_SIZE
        var currentRead = 0
        while (true) {
            while (currentRead < size) {
                currentRead += read()
            }

            inbox.flip()

            if (opcode == null) {
                currentRead -= size
                opcode = inbox.getOpcode()
                size = inbox.getSize()
                inbox.compact()
                continue
            } else {
                val buffer = bufferPool.get()
                currentRead -= size
                repeat(size) {
                    buffer.put(inbox.get())
                }
                buffer.flip()
                onPacketReceiveListeners.forEach { it(this@GClient) }
                val setOpcode = opcode
                launch(coroutineContext) {
                    packetProcessor.handlePacket(
                        setOpcode,
                        buffer,
                        this@GClient
                    )
                    bufferPool.clearAndRelease(buffer)
                }
                opcode = null
                size = OPCODE_AND_SIZE_BYTE_SIZE
                inbox.compact()
            }

        }
    }

    override suspend fun initShutdown() {
        socketChannel.shutdownInput()
        socketChannel.shutdownOutput()
        socketChannel.close()
    }


    private fun ByteBuffer.getOpcode() = get()
    private fun ByteBuffer.getSize() = int

    override fun hashCode(): Int {
        return sessionUUID?.hashCode() ?: -1 //TODO: FIX
    }

    override fun equals(other: Any?) = if (other is GClient) sessionUUID == other.sessionUUID else false
}


