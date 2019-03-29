package com.github.yona168.giraffe.net.messenger

import com.gitlab.avelyn.architecture.base.Toggleable

/**
 * Extension of [Toggleable] that allows actions to occur on enable and on disable.
 */
interface Toggled : Toggleable {
    /**
     * Register listeners to run on enable.
     * @param[listeners] the listeners to run on enable.
     * @return this for chaining.
     */
    fun onEnable(vararg listeners: Runnable): Toggled

    /**
     * Register listeners to run on disable.
     * @param[listeners] the listeners to run on disable.
     * @return this for chaining.
     */
    fun onDisable(vararg listeners: Runnable): Toggled

    @JvmDefault
    fun onEnable(function: ()->Unit)=onEnable(Runnable(function))

    @JvmDefault
    fun onDisable(function: ()->Unit)=onDisable(Runnable(function))
}