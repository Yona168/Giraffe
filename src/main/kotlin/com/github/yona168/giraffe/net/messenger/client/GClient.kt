package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.constants.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.client.GClient.Companion.Side
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.messenger.server.Server
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.QueuedOpSendablePacket
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.gitlab.avelyn.architecture.base.Component
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Runnable
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [Client]. For internal packets, this uses the opcode identifier of [INTERNAL_OPCODE].
 * Thus, NOTHING else should be registered under that opcode. Further, any implementation of [Server] that uses this class
 * should send a [SendablePacket] made EXACTLY like this one:
 *
 *     writeByte(Constants.HANDSHAKE_SUB_IDENTIFIER)
 *     writeUUID(uuidToSend)
 *
 * The [UUID] sent will be set to [sessionUUID], thus establishing the same [UUID] by both the Server and Client. Other than
 * this requirement, this class is fit for use with any [Server] implementation, or truly, even anything above that.
 *
 * This implementation uses [Component] for enabling/disabling.
 *
 *This class is also used as the [Client] implementation Server-side. Thus, some things
 * work differently on both ends. These differences are determined based on the constructor used. As such, to make that more explicit,
 * instances should be created through [GClient.newServerside] and [GClient.newClientside]
 *
 * The differences are as follows:
 * 1. server-side clients CANNOT be re-enabled, whereas client-side ones can.
 * 2. When they disable, server-side clients will NOT disable their [packetProcessor]. It is assumed that they all
 * share one packet processor from the server. If this is not the case with your server implementation, simply use [onDisable]
 * to cancel it.
 *
 * Note that this implementation expects packets to be structured as follows:
 * 1. Opcode of the packet (of type [Opcode]).
 * 2. Size of the packet (of type [Size]).
 * 3. The rest of the bytes.
 * Thus, any bytes sent with a [SendablePacket] should reflect this pattern. [QueuedOpSendablePacket] does this.
 *
 * @param[address] the [SocketAddress] that this Client will connect to when it is enabled with [Toggleable.enable].
 * @param[packetProcessor] The [PacketProcessor] that will be used to process received packets.
 * @param[socketChannel] The [AsynchronousSocketChannel] used to send and receive data.
 *@param[side] The [Side] of this client, indicating if this is a "server-side" client or "client-side" client
 */

class GClient private constructor(
    side: Side,
    address: SocketAddress?,
    packetProcessor: PacketProcessor,
    socketChannel: AsynchronousSocketChannel,
    private val timeoutMillis: Long
) : AbstractScopedPacketChannelComponent(packetProcessor), Client {

    /**
     * The [AsynchronousSocketChannel] that this client sends & receives bytes over
     */
    override val socketChannel: AsynchronousSocketChannel
        get() = backingSocketChannel

    private var backingSocketChannel: AsynchronousSocketChannel = socketChannel
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
     * The collection of listeners ran when a packet is received, registered through [onPacketReceive]
     */
    private val onPacketReceiveListeners: MutableSet<(Client)->Unit> = mutableSetOf()

    /**
     * The collection of listeners ran when the handshake packet is received, registered through [onHandshake]
     */
    private val onHandshakeListeners: MutableSet<(Client)->Unit> = mutableSetOf()

    /**
     * A [UUID] used for [hashCode], as [sessionUUID] cannot be used for that
     */
    private val identifier = UUID.randomUUID()


    /**
     * This client's [sessionUUID], as specified by the handshake packet mentioned in the class descriptor
     */
    override val sessionUUID: UUID?
        get() = backingSessionUUID

    /**
     * The backing property for [sessionUUID]
     */
    private var backingSessionUUID: UUID? = null


    /**
     * The [Side] of this client. If this client was created through [newServerside], this is [Side.Serverside]. If it was
     * created through [clientside], it's [Side.Clientside]
     */
    val side: Side
        get() = backingSide

    /**
     * the backing property for [side]
     */
    private var backingSide: Side = side

    init {
        /*
        On enable, if the socket channel is closed and the side is serverside, that means this is not the first enable. Throw an exception if so.
        If its clientside, just back a new channel and reset the session uuid
         */
        this.onEnable {
            if (!socketChannel.isOpen) {
                when (side) {
                    Side.Serverside -> throw IllegalStateException("A Server-side GClient cannot be re-enabled!")
                    Side.Clientside -> {
                        backingSocketChannel = AsynchronousSocketChannel.open()
                        backingSessionUUID = null
                    }
                }
                sequenceOf(StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_SNDBUF).forEach {
                    socketChannel.setOption(
                        it,
                        MAX_PACKET_BYTE_SIZE
                    )
                }
            }
            /*
            Grab a Read/Write handler based on side
             */
            readWriteHandler = ReadWriteHandlerSupplier(this)
            /*
            If its a client, make sure address is not null, connect, register the handshake packet handler
             */
            if (side == Side.Clientside) {
                Objects.requireNonNull(address)
                connect(address as SocketAddress)


                packetProcessor.on(INTERNAL_OPCODE) { _,packet ->
                    val opcode = packet.readByte()
                    when (opcode) {
                        HANDSHAKE_SUB_IDENTIFIER -> {
                            backingSessionUUID = packet.readUUID()
                            onHandshakeListeners.forEach { it(this) }
                        }
                    }
                }

            }
            //Start reading from channel
            loopRead()
        }
    }

    /**
     *  This constructor should be used by the [Server] ONLY, as
     * if this constructor is used, the [socketChannel] is assumed to be connected and will not connect. [sessionUUID]
     * becomes this object's [sessionUUID]. To make this difference more explicit, this constructor is declared private
     * and "server-side" clients are instead created through [GClient.newServerside]
     * @param[socketChannel] The [AsynchronousSocketChannel] that is assumed to be already connected somewhere.
     * @param[packetProcessor] The [PacketProcessor] to be used by this client.
     * @param[sessionUUID] The [UUID] to be set to [sessionUUID]
     */
    private constructor(
        socketChannel: AsynchronousSocketChannel,
        packetProcessor: PacketProcessor,
        sessionUUID: UUID
    ) : this(
        side = Side.Serverside,
        address = null,
        packetProcessor = packetProcessor,
        socketChannel = socketChannel,
        timeoutMillis = 0
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
    private constructor(address: SocketAddress, packetProcessor: PacketProcessor, timeoutMillis: Long) : this(
        side = Side.Clientside,
        address = address,
        packetProcessor = packetProcessor,
        socketChannel = AsynchronousSocketChannel.open(),
        timeoutMillis = timeoutMillis
    )


    /**
     * Connects to the server, with a given timeout
     */
    private fun connect(
        address: SocketAddress,
        unit: TimeUnit,
        timeout: Long
    ) {
        socketChannel.connect(address).get(timeout, unit)
    }

    /**
     * Connects to the server with the timeout specified in the constructor, in millis
     */
    private fun connect(address: SocketAddress): GClient {
        connect(address, TimeUnit.MILLISECONDS, timeoutMillis)
        return this
    }

    /**
     * Launches a coroutine that writes the bytes from a [SendablePacket] to this [socketChannel].
     * [SendablePacket.build] is called to obtain a [ByteBuffer], which is then read in its entirety into the channel.
     * The [controller] ensures that coroutines wait for the preceding one to complete before writing, in order to avoid
     * errors. This process is asynchronous.
     *
     * @param[packet] the [SendablePacket] to build and send.
     * @returns a [CompletableFuture] that will hold the amount of bytes sent.
     */
    override fun write(packet: SendablePacket): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        launch(coroutineContext) {
            var numSent = 0
            controller.withLock {
                val buf = packet.build()
                while (buf.hasRemaining()) {
                    numSent += aWrite(buf)
                }
            }
            future.complete(numSent)
        }
        return future
    }


    override fun onPacketReceive(func: (Client)->Unit) = apply { onPacketReceiveListeners.add(func) }

    override fun onHandshake(func: (Client)->Unit) = apply { onHandshakeListeners.add(func) }


    override fun onEnable(vararg listeners: Runnable)=apply{super<AbstractScopedPacketChannelComponent>.onEnable(*listeners)}
    override fun onEnable(listeners: ()->Unit)= apply { super<Client>.onEnable(listeners) }

    override fun onDisable(vararg listeners: Runnable)=apply{super<AbstractScopedPacketChannelComponent>.onDisable(*listeners)}
    override fun onDisable(listeners: ()->Unit):GClient = apply { super<Client>.onDisable(listeners) }

    /**
     * Reads a bunch of bytes from the channel into [inbox]. If the result is -1, which usually means
     * that the server connection has closed, this client disables as well.
     * @return the amt of bytes read
     */
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
                buffer.prepareRead()
                onPacketReceiveListeners.forEach { it(this@GClient) }
                val setOpcode = opcode
                launch(coroutineContext) {
                    packetProcessor.handleSuspend(
                        this@GClient,
                        setOpcode,
                        buffer
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
        if (this.backingSide == Side.Clientside) {
            packetProcessor.disable()
        }
    }

    private fun ByteBuffer.getOpcode() = get()
    private fun ByteBuffer.getSize() = int

    override fun hashCode() = identifier.hashCode()

    /**
     * @param[other] Object to check for equality
     * @return true if [other] is a [Client] and has the same [sessionUUID] as this client. Otherwise, returns false.
     * @see[Object.equals]
     */
    override fun equals(other: Any?): Boolean {
        if (other is Client) {
            if (this.sessionUUID != null && other.sessionUUID != null) {
                return sessionUUID == other.sessionUUID
            }
        }
        return false
    }

    companion object {

        /**
         * The "Side" (client or server) that this client is on. GServer uses GClient objects to process things server side.
         * Thus, a client can have one of two sides. If [newServerside] is used, [GClient.side] is [Side.Serverside], and if
         * [newClientside] is used, [GClient.side] is [Side.Clientside].
         */
        enum class Side {
            Serverside,
            Clientside
        }

        /**
         * Creates a [GClient] from a given [AsynchronousSocketChannel] that will not connect on enable. It also has a given
         * [sessionUUID].
         * @param[socketChannel] The already-connected [AsynchronousSocketChannel] to use.
         * @param[packetProcessor] The [PacketProcessor] to use.
         * @param[sessionUUID] The [UUID] to set [sessionUUID] to
         * @return A [GClient] to wrap the given channel.
         */
        @JvmStatic
        fun newServerside(
            socketChannel: AsynchronousSocketChannel,
            packetProcessor: PacketProcessor,
            sessionUUID: UUID
        ) =
            GClient(socketChannel, packetProcessor, sessionUUID)

        /**
         * Creates a [GClient] that opens a new [AsynchronousSocketChannel] and connects it to the address when it enables.
         * @param[address] The [SocketAddress] to connect to on enable.
         * @param[packetProcessor] The [PacketProcessor] to use.
         * @param[timeoutMillis] How long the client will wait to finish connecting before giving up (in milliseconds).
         * @return A [GClient] that, when enabled, attempts to connect to a server specified by the [address].
         */
        @JvmStatic
        fun newClientside(address: SocketAddress, packetProcessor: PacketProcessor, timeoutMillis: Long) =
            GClient(address, packetProcessor, timeoutMillis)

        /**
         * An object to supply a [ContinuationCompletionHandler] to handle reading/writing bytes across the channel.
         * [ContinuationCompletionHandler.failed] is different depending on the [Side] of this client, so this is necessary.
         */
        private object ReadWriteHandlerSupplier : (GClient) -> ContinuationCompletionHandler<Int> {
            override fun invoke(client: GClient): ContinuationCompletionHandler<Int> {
                return when (client.side) {
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
}



