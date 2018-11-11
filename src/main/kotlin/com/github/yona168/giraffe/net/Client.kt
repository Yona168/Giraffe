package com.github.yona168.giraffe.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousByteChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class Client(address: InetSocketAddress, unit: TimeUnit, timeout: Long, onTimeoutFunction: (Client) -> Unit) :
    Networker(), AsynchronousByteChannel {
    private lateinit var channel: AsynchronousSocketChannel
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun read(dst: ByteBuffer?): Future<Int> = channel.read(dst)

    init {
        onEnable {
            channel= AsynchronousSocketChannel.open()
            connectTo(address, unit, timeout, onTimeoutFunction)
        }
    }

    private fun connectTo(
        address: InetSocketAddress,
        unit: TimeUnit,
        timeout: Long,
        onTimeoutFunction: (Client) -> Unit
    ) {
        val connectionResult = runCatching<Client> {
            channel.connect(address).get(timeout, unit)
            this
        }
        connectionResult.onFailure { exc ->
            print("Cause: ${exc.cause} Message: ${exc.message}")
            when (exc) {
                is AlreadyConnectedException -> {
                    error("Can't connect when Client is already connected!")
                }
                else -> {
                    onTimeoutFunction(this)
                    close()
                }
            }
        }
    }

    override fun isOpen(): Boolean = channel.isOpen
    override fun <A : Any?> write(src: ByteBuffer?, attachment: A, handler: CompletionHandler<Int, in A>?) =
        channel.write(src, attachment, handler)

    override fun write(src: ByteBuffer?): Future<Int> = channel.write(src)
    override fun close() = channel.close()
    override fun <A : Any?> read(dst: ByteBuffer?, attachment: A, handler: CompletionHandler<Int, in A>?) =
        channel.read(dst, attachment, handler)


}

