package com.github.yona168.giraffe.net

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

fun main()=runBlocking{
val context= newSingleThreadContext("Test")
    println("Thread is ${Thread.currentThread().name}")
   val job= GlobalScope.launch(context) {
        runBlocking {
            println("Thread is ${Thread.currentThread().name}")
        }
    }
    job.join()
}