package de.fabmax.calc

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.Ray
import de.fabmax.lightgl.util.Painter

/**
 * Base class for all UI elements
 */
abstract class UiElement<T: LayoutConfig>(config: T, context: Context) {

    val context = context
    val layoutConfig = config

    val bounds: BoundingBox
        get() { return layoutConfig.bounds }
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

    var onClickListener: (() -> Unit)? = null
    var onTouchListener: ((x: Float, y: Float, action: Int) -> Unit)? = null

    var pressed = false

    abstract fun paint(painter: Painter)

    open fun doLayout(orientation: Int, parentBounds: BoundingBox, ctx: Context):
            BoundingBox {
        var props = layoutConfig.getLayout(orientation)
        props.doLayout(parentBounds, ctx)
        layoutConfig.updateBounds()
        return props.bounds
    }

    open fun mixConfigs(portLandMix: Float) {
        layoutConfig.mixConfigs(portLandMix)
    }

    open fun processTouch(ray: Ray, action: Int) {
        var dist = bounds.computeHitDistanceSqr(ray)
        if (dist < Float.MAX_VALUE) {
            // we have a hit
            dist = Math.sqrt(dist.toDouble()).toFloat()
            val x = ray.origin[0] + ray.direction[0] * dist - bounds.minX
            val y = ray.origin[1] + ray.direction[1] * dist - bounds.minY

            val pressedBefore = pressed
            pressed = action == MotionEvent.ACTION_DOWN
            if (pressed and !pressedBefore) {
                onPress(x, y)
            } else  if (!pressed and pressedBefore) {
                onRelease()
                onClickListener?.invoke()
            }

            onTouchListener?.invoke(x, y, action)
        }
    }

    open fun onPress(x: Float, y: Float) {
        // default does nothing
    }

    open fun onRelease() {
        // default does nothing
    }
}

abstract class UiElementBuilder<T: UiElement<*>>(context: Context) {

    val context = context
    val element = create()
    var orientation = Orientation.ALL

    abstract fun create(): T

    fun bounds(x: SizeSpec, y: SizeSpec, width: SizeSpec, height: SizeSpec) {
        element.layoutConfig.setLayoutFun(orientation, { b, c ->
            val w = b.sizeX
            val h = b.sizeY
            this.setBounds(x.toPx(w, h, c), y.toPx(w, h, c),
                    width.toPx(w, h, c), height.toPx(w, h, c))
        })
    }

    fun bounds(layoutFun: LayoutProperties.(parentBounds: BoundingBox, ctx: Context) -> Unit) {
        element.layoutConfig.setLayoutFun(orientation, layoutFun)
    }

    fun land(init: UiElementBuilder<T>.() -> Unit) {
        orientation = Orientation.LANDSCAPE
        init();
        orientation = Orientation.ALL
    }

    fun port(init: UiElementBuilder<T>.() -> Unit) {
        orientation = Orientation.PORTRAIT
        init();
        orientation = Orientation.ALL
    }

    fun init(initFun: T.() -> Unit) {
        element.initFun()
    }
}
