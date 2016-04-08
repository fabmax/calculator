package de.fabmax.calc

import android.util.Log
import de.fabmax.lightgl.util.GlMath

/**
 * Helper class animating stuff
 */
open class Animation<T>(start: T, end: T, value: T, mix: (f: Float, a: T, b: T, value: T) -> T) {

    private val mixFun = mix

    var start: T = start
        private set
    var end: T = end
        private set
    var value: T = value
        private set

    private var startT = 0L
    private var durationT = 0f
    private var smooth = false
    val isDone: Boolean
        get() { return (System.currentTimeMillis() - startT) / 1000f > durationT }

    fun reverse(): Animation<T> {
        val tmp = start
        start = end
        end = tmp
        return this
    }

    fun start(start: T, end: T): Animation<T> {
        this.start = start
        this.end = end
        mix(0f)
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
        this.smooth = smooth
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
        return mix(p)
    }

    private fun mix(p: Float): T {
        val f = GlMath.clamp(p, 0f, 1f)
        value = mixFun(f, start, end, value)
        return value
    }

}

class FloatAnimation : Animation<Float>(0f, 0f, 0f, { f, a, b, v -> a*(1-f) + b*f })
class Vec3fAnimation : Animation<Vec3f>(Vec3f(), Vec3f(), Vec3f(), { f, a, b, v ->
    v.x = a.x * (1-f) + b.x * f
    v.y = a.y * (1-f) + b.y * f
    v.z = a.z * (1-f) + b.z * f
    v
}) {
    fun start(startX: Float, startY: Float, startZ: Float, endX: Float, endY: Float, endZ: Float):
            Vec3fAnimation {
        start.set(startX, startY, startZ)
        end.set(endX, endY, endZ)
        return this
    }

    fun change(endX: Float, endY: Float, endZ: Float): Vec3fAnimation {
        end.set(endX, endY, endZ)
        return this
    }
}

class Vec3f {
    var x = 0f
    var y = 0f
    var z = 0f

    fun set(x: Float, y: Float, z: Float) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}