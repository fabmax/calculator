package de.fabmax.calc.ui

import android.content.Context
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.Ray
import de.fabmax.lightgl.util.Painter
import java.util.*

/**
 * Base class for UI layouts. Currently this is the only container UI element which can hold
 * child elements.
 */
class Layout(context: Context) : UiElement<LayoutConfig>(LayoutConfig(), context) {

    private val mChildComparator = Comparator<UiElement<*>> { a, b ->
        java.lang.Float.compare(a.bounds.maxZ, b.bounds.maxZ)
    }
    private val mChildren = ArrayList<UiElement<*>>()

    private val mLocalRay = Ray()

    /**
     * Adds a child UI element to this layout
     */
    fun add(child: UiElement<*>) {
        mChildren.add(child)
    }

    /**
     * Searches for a child element with the given ID, returns that element or null if no element
     * was found.
     */
    fun findById(id: String): UiElement<*>? {
        for (i in mChildren.indices) {
            val c = mChildren[i]
            if (c.id == id) {
                return c
            } else if (c is Layout) {
                val r = c.findById(id)
                if (r != null) {
                    return r
                }
            }
        }
        return null
    }

    /**
     * Lays out this layout element and all children.
     */
    override fun doLayout(orientation: Int, parentBounds: BoundingBox, ctx: Context):
            BoundingBox {
        val bounds = super.doLayout(orientation, parentBounds, ctx)
        for (elem in mChildren) {
            elem.doLayout(orientation, bounds, ctx)
        }
        return bounds
    }

    /**
     * Interpolates this layout and all children between portrait and landscape.
     */
    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)
        for (elem in mChildren) {
            elem.mixConfigs(portLandMix)
        }
    }

    /**
     * Draws all child elements.
     */
    override fun paint(painter: Painter) {
        // todo: Collections.sort is quite heap intensive...
        Collections.sort(mChildren, mChildComparator)

        // using i in IntRange saves an Iterator allocation
        for (i in mChildren.indices) {
            val elem = mChildren[i]
            painter.pushTransform()
            painter.translate(elem.x, elem.y, elem.z)

            elem.paint(painter)

            painter.commit()
            painter.reset()
            painter.popTransform()
        }
    }

    /**
     * Checks whether the touch event is relevant for one of the children elements.
     */
    override fun processTouch(ray: Ray, action: Int) {
        super.processTouch(ray, action)

        val dist = bounds.computeHitDistanceSqr(ray)
        if (dist < Float.MAX_VALUE) {
            // naive transformation of pick ray (considers only translation)
            mLocalRay.setDirection(ray.direction[0], ray.direction[1], ray.direction[2])
            mLocalRay.setOrigin(ray.origin[0] - bounds.minX, ray.origin[1] - bounds.minY,
                    ray.origin[2] - bounds.minZ)

            mChildren.forEach { c -> c.processTouch(mLocalRay, action) }
        }
    }
}

/**
 * Builder for layouts. Offers functions to add buttons, etc.
 */
class LayoutBuilder(context: Context) : UiElementBuilder<Layout>(context) {
    override fun create(): Layout {
        return Layout(context)
    }

    protected fun <T : UiElement<*>, U : UiElementBuilder<T>>
            initElement(builder: U, init: U.() -> Unit): T {
        builder.init()
        val elem = builder.element
        element.add(elem)
        return elem
    }

    /**
     * Adds a button to the layout.
     */
    fun button(init: ButtonBuilder.() -> Unit): Button =
            initElement(ButtonBuilder(context), init)

    /**
     * Adds a calculator panel to the layout.
     */
    fun calcPanel(init: CalcPanelBuilder.() -> Unit): CalcPanel =
            initElement(CalcPanelBuilder(context), init)

    /**
     * Adds a panel to the layout.
     */
    fun panel(init: PanelBuilder.() -> Unit): Panel<PanelConfig> =
            initElement(PanelBuilder(context), init)

}

/**
 * Entrance function for layout definitions.
 */
fun layout(context: Context, init: LayoutBuilder.() -> Unit): Layout {
    val builder = LayoutBuilder(context)
    builder.init()
    return builder.element
}
