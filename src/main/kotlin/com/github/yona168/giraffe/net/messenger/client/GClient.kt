package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.messenger.server.Server
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.gitlab.avelyn.architecture.base.Component
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [Client]. For internal packets, this uses the opcode identifier of [INTERNAL_OPCODE].
 * Thus, NOTHING else should be registered under that opcode. Further, any implementation of [Server] that uses this class
 * should send a [SendablePacket] made EXACTLY like this one:
 *
 *     writeByte(HANDSHAKE_SUB_IDENTIFIER)
 *     writeUUID(uuidToSend)
 *
 * The [UUID] sent will be set to [sessionUUID], thus establishing the same [UUID] by both the Server and Client. Other than
 * this requirement, this class is fit for use with any [Server] implementation, or truly, even anything above that.
 *
 * This implementation uses [Component] for enabling/disabling.
 *
 * @param[address] the [SocketAddress] that this Client will connect to when it is enabled with [Toggleable.enable].
 * @param[packetProcessor] The [PacketProcessor] that will be used to process received packets.
 * @param[socketChannel] The [AsynchronousSocketChannel] used to send and receive data.
 *
 */

class GClient private constructor(
    address: SocketAddress?,
    packetProcessor: PacketProcessor,
    override val socketChannel: AsynchronousSocketChannel
) : AbstractScopedPacketChannelComponent(packetProcessor), Client {

    /**
     * This class is also used as the [Client] implementation Server-side. Thus, some things
     * work differently on both ends. This constructor should be used by the [Server] ONLY, as
     * if this constructor is used, the [socketChannel] is assumed to be connected and will not connect. [sessionUUID]
     * becomes this object's [sessionUUID]. To make this difference more explicit, this constructor is declared private
     * and "server-side" clients are instead created through [GClient.serverside]
     * @param[socketChannel] The [AsynchronousSocketChannel] that is assumed to be already connected somewhere.
     * @param[packetProcessor] The [PacketProcessor] to be used by this client.
     * @param[sessionUUID] The [UUID] to be set to [sessionUUID]
     */
    private constructor(
        socketChannel: AsynchronousSocketChannel,
        packetProcessor: PacketProcessor,
        sessionUUID: UUID
    ) : this(
        address = null,
        packetProcessor = packetProcessor,
        socketChannel = socketChannel
    ) {
        backingSessionUUID = sessionUUID
    }

    /**
     * This constructor creates a "client-side" client, with the channel being automatically opened
     * and connected to [address] when enabled. This constructor is private to be more explicit, and should
     * be called through [GClient.clientside]
     * @param[address] The [SocketAddress] that this client will connect to
     * @param[packetProcessor] The [PacketProcessor] to be used by this client
     */
    private constructor(address: SocketAddress, packetProcessor: PacketProcessor) : this(
        address = address,
        packetProcessor = packetProcessor,
        socketChannel = AsynchronousSocketChannel.open()
    )

    /**
     * The [CoroutineContext] that this client will use for launching coroutines. In [GClient],
     * this is set to [Dispatchers.IO]+[job]
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /**
     * The [ByteBuffer] that incoming bytes are read into before being separated and processed as separate packets.
     */
    private val inbox = ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE)

    /**
     * The [Mutex] that ensures that callers to [GClient.write] are synchronized, so that errors do not occur.
     */
    private val controller = Mutex()

    /**
     * The [ContinuationCompletionHandler] that handles reading from and writing to the [socketChannel]
     */
    private lateinit var readWriteHandler: ContinuationCompletionHandler<Int>

    /**
     * A collection of
     */
    private val onPacketReceiveListeners: MutableSet<Consumer<Client>> = mutableSetOf()
    private val onHandshakeListeners: MutableSet<Consumer<Client>> = mutableSetOf()
    private val identifier = UUID.randomUUID()
    private var backingSessionUUID: UUID? = null
    override val sessionUUID: UUID?
        get() = backingSessionUUID
    private var side: Side? = null

    companion object {
        fun serverside(socketChannel: AsynchronousSocketChannel, packetProcessor: PacketProcessor, sessionUUID: UUID) =
            GClient(socketChannel, packetProcessor, sessionUUID)

        fun clientside(address: SocketAddress, packetProcessor: PacketProcessor) = GClient(address, packetProcessor)

        private object ReadWriteHandlerSupplier : (GClient) -> ContinuationCompletionHandler<Int> {
            override fun invoke(client: GClient): ContinuationCompletionHandler<Int> {
                if (client.side == null) {
                    throw IllegalStateException("Client does not have a side!")
                }
                return when (client.side as Side) {
                    Side.Serverside -> object : ContinuationCompletionHandler<Int>() {
                        override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
                            runBlocking {
                                client.disable()
                            }
                        }
                    }

                    Side.Clientside -> object : ContinuationCompletionHandler<Int>() {
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
        onEnable {
            val notConnected = !(socketChannel.isOpen && socketChannel.remoteAddress != null)
            side = if (notConnected) Side.Clientside else Side.Serverside
            readWriteHandler = ReadWriteHandlerSupplier(this)
            if (side is Side.Clientside) {
                Objects.requireNonNull(address)
                connect(address as SocketAddress)
                packetProcessor.on(INTERNAL_OPCODE, BiConsumer { packet, _ ->
                    val opcode = packet.readByte()
                    when (opcode) {
                        HANDSHAKE_SUB_IDENTIFIER -> {
                            backingSessionUUID = packet.readUUID()
                            onHandshakeListeners.forEach { it.accept(this) }
                        }
                    }
                })

            }
            loopRead()
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


    override fun onPacketReceive(func: Consumer<Client>) = onPacketReceiveListeners.add(func)

    override fun onHandshake(func: Consumer<Client>) = onHandshakeListeners.add(func)

    override fun onEnable(vararg listeners: Runnable) = this.apply { super.onEnable(*listeners) }
    override fun onDisable(vararg listeners: Runnable) = this.apply { super.onDisable(*listeners) }

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
        while (this@GClient.isActive) {
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
                onPacketReceiveListeners.forEach { it.accept(this@GClient) }
                val setOpcode = opcode
                launch(coroutineContext) {
                    packetProcessor.handle(
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

    override suspend fun initClose() {
        if (this.side is Side.Clientside) {
            packetProcessor.disable()
        }
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


