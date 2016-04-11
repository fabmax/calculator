package de.fabmax.calc.ui

import android.content.Context
import android.view.MotionEvent
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.Ray
import de.fabmax.lightgl.util.Painter

/**
 * Base class for all UI elements.
 */
abstract class UiElement<T: LayoutConfig>(config: T, context: Context) {

    var id: String? = null
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

    /**
     * Called whenever a touch event is detected for this UI element
     */
    var onTouchListener: ((x: Float, y: Float, action: Int) -> Unit)? = null

    /**
     * Called when a click on this UI element was detected
     */
    var onClickListener: (() -> Unit)? = null


    var pressed = false
    private var mPressTime = 0L

    /**
     * Called during frame rendering to draw the contents of this UiElement. If shadow rendering
     * is enabled, this method is actually called twice per frame. To determine between depth
     * pass and draw pass [Painter.glContext.state.isPrePass] can be used.
     */
    abstract fun paint(painter: Painter)

    /**
     * Computes the bounds of this element for the given orientation and parent size.
     */
    open fun doLayout(orientation: Int, parentBounds: BoundingBox, ctx: Context):
            BoundingBox {
        var props = layoutConfig.getLayout(orientation)
        props.doLayout(parentBounds, ctx)
        layoutConfig.updateBounds()
        return props.bounds
    }

    /**
     * Interpolates bounds between portrait and landscape orientation.
     */
    open fun mixConfigs(portLandMix: Float) {
        layoutConfig.mixConfigs(portLandMix)

        // this is a pretty nasty hack needed to prevent buttons stay in the pressed state
        if (pressed && System.currentTimeMillis() - mPressTime > 1000) {
            pressed = false
            onRelease()
        }
    }

    /**
     * Processes touch input (in a very basic way).
     */
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
            }

            // forward touch event to listener (if there is one)
            onTouchListener?.invoke(x, y, action)
        }
    }

    /**
     * UI element was pressed (touch action_down) at the specified location.
     */
    open fun onPress(x: Float, y: Float) {
        mPressTime = System.currentTimeMillis()
    }

    /**
     * UI element was released (touch action_up) at the specified location.
     */
    open fun onRelease() {
        // notify listener about click event
        onClickListener?.invoke()
    }
}

/**
 * Base builder for UI element builders. Sets the foundation for specifying element bounds in
 * portrait and landscape orientations.
 */
abstract class UiElementBuilder<T: UiElement<*>>(context: Context) {
    val context = context
    val element = create()
    var orientation = Orientation.ALL

    abstract fun create(): T

    /**
     * Sets the 2D element bounds for landscape, portrait or both orientations.
     * Z and depth are set to zero.
     */
    fun bounds(x: SizeSpec, y: SizeSpec, width: SizeSpec, height: SizeSpec) {
        element.layoutConfig.setLayoutFun(orientation, { b, c ->
            val w = b.sizeX
            val h = b.sizeY
            this.setBounds(x.toPx(w, h, c), y.toPx(w, h, c),
                    width.toPx(w, h, c), height.toPx(w, h, c))
        })
    }

    /**
     * Sets the 3D element bounds for landscape, portrait or both orientations.
     */
    fun bounds(x: SizeSpec, y: SizeSpec, z: SizeSpec,
               width: SizeSpec, height: SizeSpec, depth: SizeSpec) {
        element.layoutConfig.setLayoutFun(orientation, { b, c ->
            val w = b.sizeX
            val h = b.sizeY
            this.setBounds(x.toPx(w, h, c), y.toPx(w, h, c), z.toPx(w, h, c),
                    width.toPx(w, h, c), height.toPx(w, h, c), depth.toPx(w, h, c))
        })
    }

    /**
     * Sets a custom layout function, usually the other bounds functions are sufficient.
     */
    fun bounds(layoutFun: LayoutProperties.(parentBounds: BoundingBox, ctx: Context) -> Unit) {
        element.layoutConfig.setLayoutFun(orientation, layoutFun)
    }

    /**
     * Sets this builder into landscape mode. Settings made in this mode only affect the landscape
     * orientation.
     */
    fun land(init: UiElementBuilder<T>.() -> Unit) {
        orientation = Orientation.LANDSCAPE
        init();
        orientation = Orientation.ALL
    }

    /**
     * Sets this builder into portrait mode. Settings made in this mode only affect the portrait
     * orientation.
     */
    fun port(init: UiElementBuilder<T>.() -> Unit) {
        orientation = Orientation.PORTRAIT
        init();
        orientation = Orientation.ALL
    }

    /**
     * Sets up the created UI element.
     */
    fun init(initFun: T.() -> Unit) {
        element.initFun()
    }
}
