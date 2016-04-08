package de.fabmax.calc.ui

import android.content.Context
import android.util.Log
import de.fabmax.calc.ui.BoundsInterpolator
import de.fabmax.calc.ui.LinearBoundsInterpolator
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.util.GlMath

/**
 * Layout for different configurations, currently only portrait and landscape are available
 */
open class LayoutConfig {

    private val layouts = Array(Orientation.ALL, { i -> LayoutProperties() })

    var portLandMix: Float = 1f
        private set

    var boundsInterpolator: BoundsInterpolator
    val bounds: BoundingBox
        get() { return boundsInterpolator.bounds }

    init {
        boundsInterpolator = LinearBoundsInterpolator(
                layouts[Orientation.PORTRAIT].bounds, layouts[Orientation.LANDSCAPE].bounds)
    }

    fun getLayout(orientation: Int): LayoutProperties {
        return layouts[orientation]
    }

    fun setLayoutFun(orientation: Int,
                     layoutFun: LayoutProperties.(parentBounds: BoundingBox, ctx: Context) -> Unit) {
        if (orientation >= 0 && orientation < layouts.size) {
            layouts[orientation].layoutFun = layoutFun
        } else {
            layouts.forEach { p -> p.layoutFun = layoutFun }
        }
    }

    fun translate(orientation: Int, tX: Float, tY: Float, tZ: Float) {
        if (orientation >= 0 && orientation < layouts.size) {
            val bb = layouts[orientation].bounds
            bb.minX += tX
            bb.maxX += tX
            bb.minY += tY
            bb.maxY += tY
            bb.minZ += tZ
            bb.maxZ += tZ
        } else {
            layouts.forEach { p ->
                val bb = p.bounds
                bb.minX += tX
                bb.maxX += tX
                bb.minY += tY
                bb.maxY += tY
                bb.minZ += tZ
                bb.maxZ += tZ
            }
        }
    }

    fun updateBounds() {
        boundsInterpolator.update(portLandMix)
    }

    open fun mixConfigs(portLandMix: Float) {
        this.portLandMix = GlMath.clamp(portLandMix, 0f, 1f)
        boundsInterpolator.update(this.portLandMix)
    }
}

class LayoutProperties {
    val bounds = BoundingBox(0.0f, 0.0f, 0.0f)
    val x: Float
        get() = bounds.minX
    val y: Float
        get() = bounds.minY
    val z: Float
        get() = bounds.minZ
    val width: Float
        get() = bounds.maxX - bounds.minX
    val height: Float
        get() = bounds.maxY - bounds.minY
    val depth: Float
        get() = bounds.maxZ - bounds.minZ

    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        setBounds(x, y, 0.0f, width, height, 0.0f)
    }

    fun setBounds(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        bounds.minX = x
        bounds.minY = y
        bounds.minZ = z
        bounds.maxX = bounds.minX + width
        bounds.maxY = bounds.minY + height
        bounds.maxZ = bounds.minZ + depth
    }

    var layoutFun: LayoutProperties.(BoundingBox, Context) -> Unit = { b, c -> }

    fun doLayout(parentBounds: BoundingBox, ctx: Context) {
        layoutFun(parentBounds, ctx)
    }
}

class Orientation private constructor() {
    companion object {
        val PORTRAIT = 0
        val LANDSCAPE = 1

        val ALL = 2
    }
}
