package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.Toggled
import com.gitlab.avelyn.architecture.base.Component


fun Component.onEnable(function: () -> Unit): Component =
    this.onEnable(function)

fun Component.onDisable(function: () -> Unit): Component =
    this.onDisable(function)



