package com.github.yona168.giraffe.net.messenger

import com.gitlab.avelyn.architecture.base.Toggleable

interface Toggled : Toggleable {
    fun onEnable(vararg listeners: Runnable): Toggled
    fun onDisable(vararg listeners: Runnable): Toggled
}