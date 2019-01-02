package com.github.yona168.giraffe.net.messenger.client

import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import kotlin.coroutines.coroutineContext

interface IReadWriteController {
    suspend fun controlledAccess(func: suspend () -> Int): Int
}

class ReadWriteController : IReadWriteController {
    private val mutexController = Mutex()
    override suspend fun controlledAccess(func: suspend () -> Int): Int {
        return mutexController.withLock(coroutineContext) {
            func()
        }
    }
}

