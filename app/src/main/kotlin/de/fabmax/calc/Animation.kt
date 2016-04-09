package de.fabmax.calc

import android.util.Log
import de.fabmax.lightgl.util.GlMath

/**
 * Helper class animating stuff
 */
open class Animation<T>(start: T, end: T, value: T, mix: (f: Float, a: T, b: T, value: T) -> T) {

    private val mMixFun = mix

    protected var mStart: T = start
    protected var mEnd: T = end
    protected var mValue: T = value

    private var mStartT = 0L
    private var mDurationT = 0f
    private var mSmooth = false

    var isDone = true
        private set

    var whenDone: (() -> Unit)? = null

    open fun reverse(): Animation<T> {
        val start = mStart
        val end = mEnd
        set(end, start)
        return this
    }

    open fun set(start: T, end: T): Animation<T> {
        this.mStart = start
        this.mEnd = end
        mix(0f)
        return this
    }

    open fun change(end: T): Animation<T> {
        this.mStart = this.mValue
        this.mEnd = end
        return this
    }

    fun start(duration: Float): Animation<T> {
        return start(duration, false)
    }

    fun start(duration: Float, smooth: Boolean): Animation<T> {
        this.mSmooth = smooth
        mDurationT = duration
        mStartT = System.currentTimeMillis()
        isDone = false
        return this
    }

    fun animate(): T {
        if (!isDone) {
            val t = System.currentTimeMillis()
            val secs = (t - mStartT).toFloat() / 1000.0f

            var p = 1f;
            if (mDurationT > 0.001f) {
                p = GlMath.clamp(secs / mDurationT, 0f, 1f)
            }
            if (mSmooth) {
                p = (1f - Math.cos(p * Math.PI).toFloat()) / 2f
            }

            mix(p)
            if (secs >= mDurationT) {
                isDone = true
                whenDone?.invoke()
            }
        }
        return mValue
    }

    protected fun mix(p: Float) {
        val f = GlMath.clamp(p, 0f, 1f)
        mValue = mMixFun(f, mStart, mEnd, mValue)
    }

}

class FloatAnimation : Animation<Float>(0f, 0f, 0f, { f, a, b, v -> a*(1-f) + b*f })
class Vec3fAnimation : Animation<Vec3f>(Vec3f(), Vec3f(), Vec3f(), { f, a, b, v ->
    v.x = a.x * (1-f) + b.x * f
    v.y = a.y * (1-f) + b.y * f
    v.z = a.z * (1-f) + b.z * f
    v
}) {
    fun set(startX: Float, startY: Float, startZ: Float, endX: Float, endY: Float, endZ: Float):
            Vec3fAnimation {
        mStart.set(startX, startY, startZ)
        mEnd.set(endX, endY, endZ)
        mix(0f)
        return this
    }

    override fun set(start: Vec3f, end: Vec3f): Animation<Vec3f> {
        mStart.set(start)
        mEnd.set(end)
        mix(0f)
        return this
    }

    fun change(endX: Float, endY: Float, endZ: Float): Vec3fAnimation {
        mStart.set(mValue)
        mEnd.set(endX, endY, endZ)
        return this
    }

    override fun change(end: Vec3f): Animation<Vec3f> {
        mStart.set(mValue)
        mEnd.set(end)
        return this
    }

    override fun reverse(): Animation<Vec3f> {
        mValue.set(mEnd)
        mEnd.set(mStart)
        mStart.set(mValue)
        return this
    }
}

class Vec3f {
    var x = 0f
    var y = 0f
    var z = 0f

    fun set(v: Vec3f) {
        x = v.x
        y = v.y
        z = v.z
    }

    fun set(x: Float, y: Float, z: Float) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}