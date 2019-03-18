package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.packet.HANDSHAKE_SUB_IDENTIFIER
import com.github.yona168.giraffe.net.packet.INTERNAL_OPCODE
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * The basic implementation of
 */

class GClient constructor(
    address: SocketAddress?,
    override val packetProcessor: ScopedPacketProcessor,
    override val socketChannel: AsynchronousSocketChannel,
    private val closer: ((IClient) -> Unit)?
) : Client(packetProcessor) {
    constructor(
        socketChannel: AsynchronousSocketChannel,
        packetProcessor: ScopedPacketProcessor,
        sessionUUID: UUID,
        closer: ((IClient) -> Unit)?
    ) : this(
        address = null,
        packetProcessor = packetProcessor,
        socketChannel = socketChannel,
        closer = closer
    ) {
        backingSessionUUID = sessionUUID
    }

    constructor(address: SocketAddress, packetProcessor: ScopedPacketProcessor) : this(
        address = address,
        packetProcessor = packetProcessor,
        socketChannel = AsynchronousSocketChannel.open(),
        closer = null
    )

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val inbox = ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE)
    private val controller = Mutex()

    private lateinit var readWriteHandler: ContinuationCompletionHandler<Int>

    private val preEnable: MutableSet<(IClient) -> Unit> = mutableSetOf()
    private val preDisconnectListeners: MutableSet<(IClient) -> Unit> = mutableSetOf()
    private val postDisconnectListeners: MutableSet<(IClient) -> Unit> = mutableSetOf()
    private val onPacketReceiveListeners: MutableSet<(IClient) -> Unit> = mutableSetOf()
    private val onHandshakeListeners: MutableSet<(IClient) -> Unit> = mutableSetOf()
    private val identifier = UUID.randomUUID()
    private var backingSessionUUID: UUID? = null
    override val sessionUUID: UUID?
        get() = backingSessionUUID
    private var side: com.github.yona168.giraffe.net.messenger.client.Side? = null

    companion object {
        private object ReadWriteHandlerSupplier : (GClient) -> ContinuationCompletionHandler<Int> {
            override fun invoke(client: GClient): ContinuationCompletionHandler<Int> {
                if (client.side == null) {
                    throw IllegalStateException("Client does not have a side!")
                }
                return when (client.side as Side) {
                    Serverside -> object : ContinuationCompletionHandler<Int>() {
                        override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
                            runBlocking {
                                client.disable()
                            }
                        }
                    }

                    Clientside -> object : ContinuationCompletionHandler<Int>() {
                        override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
                            val message = exc.message
                            message ?: return
                            if (message.startsWith("The specified network name is no longer available.")) {
                                runBlocking {
                                    client.disable()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        addChild(packetProcessor)
        onEnable {
            val notConnected = !(socketChannel.isOpen && socketChannel.remoteAddress != null)
            side = if (notConnected) Clientside else Serverside
            readWriteHandler = ReadWriteHandlerSupplier(this)
            if (side is Clientside) {
                Objects.requireNonNull(address)
                connect(address as SocketAddress)
                packetProcessor.reigster(INTERNAL_OPCODE) { packet, _ ->
                    val opcode = packet.readByte()
                    when (opcode) {
                        HANDSHAKE_SUB_IDENTIFIER -> {
                            backingSessionUUID = packet.readUUID()
                            onHandshakeListeners.forEach { it(this) }
                        }
                    }
                }

            }
            loopRead()
            preEnable.forEach { it(this) }
        }
    }

    private fun connect(
        address: SocketAddress,
        unit: TimeUnit,
        timeout: Long
    ) {
            socketChannel.connect(address).get(timeout, unit)
    }

    fun connect(address: SocketAddress): GClient {
        connect(address, TimeUnit.MINUTES, 2)
        return this
    }

    override fun write(packet: SendablePacket) = launch(coroutineContext) {
        controller.withLock {
            val buf = packet.build()
            while (buf.hasRemaining()) {
                aWrite(buf)
            }
        }
    }


    override fun preEnable(func: (IClient) -> Unit) = preEnable.add(func)

    override fun preDisconnect(func: (IClient) -> Unit) = preDisconnectListeners.add(func)

    override fun postDisconnect(func: (IClient) -> Unit) = postDisconnectListeners.add(func)

    override fun onPacketReceive(func: (IClient) -> Unit) = onPacketReceiveListeners.add(func)

    override fun onHandshake(func: (IClient) -> Unit) = onHandshakeListeners.add(func)


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
        socketChannel.read(buf, cont, readWriteHandler)
    }

    private suspend fun aWrite(buf: ByteBuffer): Int = suspendCancellableCoroutine {
        socketChannel.write(buf, it, readWriteHandler)
    }


    private fun loopRead() = launch(coroutineContext + Dispatchers.Default) {
        var opcode: Opcode? = null
        var size = OPCODE_AND_SIZE_BYTE_SIZE
        var currentRead = 0
        while (isActive) {
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

    override suspend fun close() {
        preDisconnectListeners.forEach { it(this) }
        closer?.invoke(this)
        Objects.requireNonNull(sessionUUID)
        if (socketChannel.isOpen) {
            socketChannel.close()
        }
        cancelCoroutines()
        postDisconnectListeners.forEach { it(this) }
    }

    private fun ByteBuffer.getOpcode() = get()
    private fun ByteBuffer.getSize() = int

    override fun hashCode() = identifier.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is GClient && identifier == other.identifier) {
            if (this.sessionUUID != null && other.sessionUUID != null) {
                return sessionUUID == other.sessionUUID
            }
            return true
        }
        return false
    }
}


