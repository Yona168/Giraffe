package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.messenger.server.SESSION_UUID_PACKET_OPCODE
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class Client @JvmOverloads constructor(
    packetProcessor: ScopedPacketProcessor,
    override val socketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) : AbstractScopedPacketChannelComponent(packetProcessor),
    Writable {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val inbox = ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE)
    private val controller = Mutex()

    private object ReadCompletionHandler : ContinuationCompletionHandler<Int>()
    private object WriteCompletionHandler : ContinuationCompletionHandler<Int>()

    private val onConnectListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private val onDisconnectListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private val onPacketReceiveListeners: MutableSet<(Client) -> Unit> = mutableSetOf()
    private var backingSessionUUID: UUID? = null
    val sessionUUID:UUID?
        get() = backingSessionUUID

    init {
        onEnable {
            loopRead()
            onHandshake{packet, _->
                backingSessionUUID=UUID(packet.readLong(), packet.readLong())
            }
        }
    }

    fun connectBlocking(
        address: SocketAddress,
        unit: TimeUnit,
        timeout: Long,
        onTimeoutFunction: (Client) -> Unit
    ) {
        val connectionResult = runCatching<Client> {
            socketChannel.connect(address).get(timeout, unit)
            this
        }
        connectionResult.onSuccess { doOnConnects() }
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

    fun connectBlocking(address: SocketAddress): Client {
        connectBlocking(address, TimeUnit.DAYS, 1) {}
        return this
    }

    fun connectNonBlocking(address: SocketAddress) {
        socketChannel.connect(address, Unit, object : CompletionHandler<Void, Unit> {
            override fun completed(result: Void?, attachment: Unit?) {
                doOnConnects()
            }

            override fun failed(exc: Throwable, attachment: Unit?) {
                throw exc
            }

        })
    }

    suspend fun connectWithContinuation(address: SocketAddress) = suspendCancellableCoroutine<Client> {
        connectBlocking(address)
        doOnConnects()
        it.resumeWith(Result.success(this))
    }

    private fun doOnConnects() = onConnectListeners.forEach { it(this) }

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

    fun onConnect(func: (Client) -> Unit) = onConnectListeners.add(func)
    fun onConnect(func: () -> Unit) = onConnect { _: Client -> func() }
    fun onConnect(func: Runnable) = onConnect { _: Client -> func.run() }
    fun onConnect(func: Consumer<Client>) = onConnect { client -> func.accept(client) }

    fun onDisconnect(func: (Client) -> Unit) = onDisconnectListeners.add(func)
    fun onDisconnect(func: () -> Unit) = onDisconnect { _: Client -> func() }
    fun onDisconnect(func: Runnable) = onDisconnect { _: Client -> func.run() }
    fun onDisconnect(func: Consumer<Client>) = onDisconnect { client -> func.accept(client) }

    fun onPacketReceive(func: (Client) -> Unit) = onPacketReceiveListeners.add(func)
    fun onPacketReceive(func: () -> Unit) = onPacketReceive { _: Client -> func() }
    fun onPacketReceive(func: Runnable) = onPacketReceive { _: Client -> func.run() }
    fun onPacketReceive(func: Consumer<Client>) = onPacketReceive { client -> func.accept(client) }

    fun onHandshake(func: PacketHandlerFunction) = packetProcessor.registerHandler(SESSION_UUID_PACKET_OPCODE, func)
    fun onHandshake(func: BiConsumer<ReceivablePacket, Writable>) =
        onHandshake { packet, client -> func.accept(packet, client) }

    fun enableOnConnect() = onConnect { _ -> enable() }

    private suspend fun read(): Int {
        val read = withContext(coroutineContext) {
            read(inbox)
        }
        return when (read) {
            -1 -> {
                this@Client.disable()
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


    private fun loopRead() = launch(coroutineContext+Dispatchers.Default) {
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
                onPacketReceiveListeners.forEach { it(this@Client) }
                val setOpcode = opcode
                launch(coroutineContext) {
                    packetProcessor.handlePacket(
                        setOpcode,
                        buffer,
                        this@Client
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


    private fun ByteBuffer.getOpcode() = short
    private fun ByteBuffer.getSize() = int

    override fun hashCode(): Int {
        return sessionUUID?.hashCode()?:-1 //TODO: FIX
    }

    override fun equals(other: Any?)=if(other is Client) sessionUUID==other.sessionUUID else false
}


