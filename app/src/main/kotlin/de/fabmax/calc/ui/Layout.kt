package de.fabmax.calc.ui

import android.content.Context
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.Ray
import de.fabmax.lightgl.util.Painter
import java.util.*

/**
 * Base class for UI layouts.
 */
class Layout(context: Context) : UiElement<LayoutConfig>(LayoutConfig(), context) {

    private val childComparator = Comparator<UiElement<*>> { a, b ->
        java.lang.Float.compare(a.bounds.maxZ, b.bounds.maxZ)
    }
    private val children = ArrayList<UiElement<*>>()

    private val localRay = Ray()

    fun add(child: UiElement<*>) {
        children.add(child)
    }

    override fun doLayout(orientation: Int, parentBounds: BoundingBox, ctx: Context):
            BoundingBox {
        val bounds = super.doLayout(orientation, parentBounds, ctx)
        for (elem in children) {
            elem.doLayout(orientation, bounds, ctx)
        }
        return bounds
    }

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)
        for (elem in children) {
            elem.mixConfigs(portLandMix)
        }
    }

    override fun paint(painter: Painter) {
        Collections.sort(children, childComparator)
        for (elem in children) {
            painter.pushTransform()
            painter.translate(elem.x, elem.y, elem.z)

            elem.paint(painter)

            painter.commit()
            painter.reset()
            painter.popTransform()
        }
    }

    override fun processTouch(ray: Ray, action: Int) {
        super.processTouch(ray, action)

        val dist = bounds.computeHitDistanceSqr(ray)
        if (dist < Float.MAX_VALUE) {
            // naive transformation of pick ray (considers only translation)
            localRay.setDirection(ray.direction[0], ray.direction[1], ray.direction[2])
            localRay.setOrigin(ray.origin[0] - bounds.minX, ray.origin[1] - bounds.minY,
                    ray.origin[2] - bounds.minZ)

            children.forEach { c -> c.processTouch(localRay, action) }
        }
    }
}

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

    fun button(init: ButtonBuilder.() -> Unit): Button =
            initElement(ButtonBuilder(context), init)

    fun calcPanel(init: CalcPanelBuilder.() -> Unit): CalcPanel =
            initElement(CalcPanelBuilder(context), init)

    fun panel(init: PanelBuilder.() -> Unit): Panel<PanelConfig> =
            initElement(PanelBuilder(context), init)

}

fun layout(context: Context, init: LayoutBuilder.() -> Unit): Layout {
    val builder = LayoutBuilder(context)
    builder.init()
    return builder.element
}
