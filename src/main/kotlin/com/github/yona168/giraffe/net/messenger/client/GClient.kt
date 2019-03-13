package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.packet.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import kotlin.coroutines.CoroutineContext

/**
 * The basic implementation of
 */
@ExperimentalCoroutinesApi
class GClient constructor(
    address: SocketAddress?,
    packetProcessor: ScopedPacketProcessor,
    override val socketChannel: AsynchronousSocketChannel
) : AbstractScopedPacketChannelComponent(packetProcessor),
    Client {

    constructor(
        socketChannel: AsynchronousSocketChannel,
        packetProcessor: ScopedPacketProcessor,
        sessionUUID: UUID
    ) : this(
        address = null,
        packetProcessor = packetProcessor,
        socketChannel = socketChannel
    ) {
        backingSessionUUID = sessionUUID
    }

    constructor(address: SocketAddress, packetProcessor: ScopedPacketProcessor) : this(
        address = address,
        packetProcessor = packetProcessor,
        socketChannel = AsynchronousSocketChannel.open()
    )

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
    private val identifier = UUID.randomUUID()
    private var waitingToShutDown = false
    private var backingSessionUUID: UUID? = null
    val sessionUUID: UUID?
        get() = backingSessionUUID

    init {
        onEnable {
            if (socketChannel.isOpen && socketChannel.remoteAddress != null) {
                Objects.requireNonNull(address)
                connect(address as SocketAddress)
            }
            packetProcessor.reigster(INTERNAL_OPCODE) { packet, client ->
                val opcode = packet.readByte()
                when (opcode) {
                    HANDSHAKE_SUB_IDENTIFIER -> onHandshakeListeners.forEach { it(packet, this) }
                    DISCONNECT_CONFIRMATION_SUB_IDENTIFIER -> waitingToShutDown = false
                    ASK_TO_DISCONNECT -> disable()
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
        timeout: Long
    ) {
        val connectionResult = runCatching<GClient> {
            socketChannel.connect(address).get(timeout, unit)
            this
        }
        connectionResult.onSuccess { onConnectListeners.forEach { it(this) } }
        connectionResult.onFailure { exc -> throw exc }
    }

    fun connect(address: SocketAddress): GClient {
        connect(address, TimeUnit.MINUTES, 2)
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

    override fun onDisconnect(func: (Client) -> Unit) = onDisconnectListeners.add(func)
    override fun onDisconnect(func: () -> Unit) = onDisconnect { _: Client -> func() }

    override fun onPacketReceive(func: (Client) -> Unit) = onPacketReceiveListeners.add(func)
    override fun onPacketReceive(func: () -> Unit) = onPacketReceive { _: Client -> func() }

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

    override suspend fun prepareShutdown(): Unit {
        Objects.requireNonNull(sessionUUID)
        write(disconnectRequest(sessionUUID as UUID))
        while (!waitingToShutDown) {
            yield()
        }
    }

    override suspend fun initShutdown() {
        if (sessionUUID != null) {
            write(disconnectRequest(sessionUUID as UUID))
        }
        socketChannel.shutdownInput()
        socketChannel.shutdownOutput()
        socketChannel.close()
    }


    private fun ByteBuffer.getOpcode() = get()
    private fun ByteBuffer.getSize() = int

    override fun hashCode() = identifier.hashCode()

    override fun equals(other: Any?) = other is GClient && identifier == other.identifier
}


