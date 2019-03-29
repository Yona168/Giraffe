package com.github.yona168.giraffe.net

import com.gitlab.avelyn.architecture.base.Component


fun Component.onEnable(function: () -> Unit): Component =
    onEnable(Runnable(function))

fun Component.onDisable(function: () -> Unit): Component =
    onDisable(Runnable(function))



