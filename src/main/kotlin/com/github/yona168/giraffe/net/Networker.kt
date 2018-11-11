package com.github.yona168.giraffe.net

import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*

abstract class Networker: Component(),CoroutineScope{
    lateinit var job: Job
 init{
     onEnable{
         job=Job()
     }
     onDisable{
         runBlocking {
             job.cancelChildren()
             job.cancelAndJoin()
         }
     }
 }


}