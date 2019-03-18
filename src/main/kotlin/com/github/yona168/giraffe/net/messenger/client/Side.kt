package com.github.yona168.giraffe.net.messenger.client

sealed class Side {
    object Serverside : Side()
    object Clientside : Side()
}
