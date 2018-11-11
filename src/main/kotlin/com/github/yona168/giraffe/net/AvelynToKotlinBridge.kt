package com.github.yona168.giraffe.net

import com.gitlab.avelyn.architecture.base.Component
import com.gitlab.avelyn.architecture.base.Parent
import com.gitlab.avelyn.architecture.base.Toggleable


fun Component.onEnable(function: () -> Unit): Component = this.onEnable(Runnable { function() })
fun Component.onDisable(function: ()->Unit):Component=this.onDisable(Runnable{function()})
fun Parent<Toggleable>.ktAddChild(o: Toggleable): Toggleable = this.addChild(o)
