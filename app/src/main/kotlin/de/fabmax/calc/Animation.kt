package de.fabmax.calc

import de.fabmax.lightgl.util.GlMath

/**
 * Helper class animating stuff
 */
open class Animation<T>(start: T, end: T, add: T.(value: T) -> T, scale: T.(value: Float) -> T) {

    private val addFun = add
    private val scaleFun = scale

    var start: T = start
        private set
    var end: T = end
        private set
    var value: T = start
        private set

    private var startT = 0L
    private var durationT = 0f
    private var smooth = false
    val isDone: Boolean
        get() { return (System.currentTimeMillis() - startT) / 1000f > durationT }

    fun start(start: T, end: T): Animation<T> {
        this.start = start
        this.end = end
        this.value = start
        return this
    }

    fun change(end: T): Animation<T> {
        this.start = this.value
        this.end = end
        return this
    }

    fun overTime(duration: Float): Animation<T> {
        return overTime(duration, false)
    }

    fun overTime(duration: Float, smooth: Boolean): Animation<T> {
        durationT = duration
        startT = System.currentTimeMillis()
        return this
    }

    fun animate(): T {
        val t = System.currentTimeMillis()
        val secs = (t - startT).toFloat() / 1000.0f

        var p = GlMath.clamp(secs / durationT, 0f, 1f)
        if (smooth) {
            p = (1f - Math.cos(p * Math.PI).toFloat()) / 2f
        }
        return animate(p)
    }

    fun animate(p: Float): T {
        val f = GlMath.clamp(p, 0f, 1f)
        value = start.addFun(end.addFun(start.scaleFun(-1f)).scaleFun(p))
        return value
    }

}

class FloatAnimation : Animation<Float>(0f, 0f, {f -> this + f}, {f -> this * f})