package de.fabmax.calc.ui

import android.content.Context
import java.util.*

/**
 * Various relative and absolute size specification for layouts. dp size specs correspond to the
 * usual android dp units. However, since usually a perspective camera is used, dp units are only
 * pixel perfect at z = 0.
 */
abstract class SizeSpec {

    private val minusSz = ArrayList<SizeSpec>()
    private var plusSz = ArrayList<SizeSpec>()

    abstract fun convert(parentWidth: Float, parentHeight: Float, ctx: Context): Float

    fun toPx(parentWidth: Float, parentHeight: Float, ctx: Context): Float {
        var px = convert(parentWidth, parentHeight, ctx)
        minusSz.forEach { sz -> px -= sz.toPx(parentWidth, parentHeight, ctx) }
        plusSz.forEach { sz -> px += sz.toPx(parentWidth, parentHeight, ctx) }
        return px
    }

    operator fun minus(other: SizeSpec): SizeSpec {
        minusSz.add(other)
        return this
    }

    operator fun plus(other: SizeSpec): SizeSpec {
        plusSz.add(other)
        return this
    }
}

fun px(px: Float): SizeSpec {
    return SizeSpecPx(px)
}

fun dp(dp: Float): SizeSpec {
    return SizeSpecDp(dp)
}

fun dp(dp: Float, ctx: Context): Float {
    return dp * ctx.resources.displayMetrics.density
}

fun rw(rw: Float): SizeSpec {
    return SizeSpecRelWidth(rw)
}

fun parentW(): SizeSpec {
    return rw(1f)
}

fun rw(rw: Float, parentW: Float): Float {
    return parentW * rw
}

fun rh(rh: Float): SizeSpec {
    return SizeSpecRelHeight(rh)
}

fun parentH(): SizeSpec {
    return rh(1f)
}

fun rh(rh: Float, parentH: Float): Float {
    return parentH * rh
}

class SizeSpecPx(px: Float) : SizeSpec() {
    val px = px
    override fun convert(parentWidth: Float, parentHeight: Float, ctx: Context): Float {
        return px
    }
}

class SizeSpecDp(dp: Float) : SizeSpec() {
    val dp = dp
    override fun convert(parentWidth: Float, parentHeight: Float, ctx: Context): Float {
        return dp * ctx.resources.displayMetrics.density
    }
}

class SizeSpecRelWidth(relWidth: Float) : SizeSpec() {
    val relWidth = relWidth
    override fun convert(parentWidth: Float, parentHeight: Float, ctx: Context): Float {
        return relWidth * parentWidth
    }
}

class SizeSpecRelHeight(relHeight: Float) : SizeSpec() {
    val relHeight = relHeight
    override fun convert(parentWidth: Float, parentHeight: Float, ctx: Context): Float {
        return relHeight * parentHeight
    }
}
