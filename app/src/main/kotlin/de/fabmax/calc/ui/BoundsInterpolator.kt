package de.fabmax.calc.ui

import de.fabmax.lightgl.BoundingBox

/**
 * Interpolator for morphing UI element BoundingBoxes between portrait and landscape configurations.
 */
abstract class BoundsInterpolator(boundsA: BoundingBox, boundsB: BoundingBox) {
    protected val boundsA = boundsA
    protected val boundsB = boundsB

    val bounds = BoundingBox(0f, 0f, 0f)

    init {
        bounds.set(boundsA)
    }

    /**
     * Compute BoundingBox for the given mix value (0 (landscape) .. 1 (portrait))
     */
    abstract fun update(mix: Float)

}

/**
 * Interpolates all BoundingBox dimensions strictly linear.
 */
open class LinearBoundsInterpolator(boundsA: BoundingBox, boundsB: BoundingBox) :
        BoundsInterpolator(boundsA, boundsB) {

    constructor(cfg: LayoutConfig) : this(cfg.getLayout(Orientation.PORTRAIT).bounds,
            cfg.getLayout(Orientation.LANDSCAPE).bounds)

    protected fun mix(a: Float, b: Float, mix: Float): Float {
        return a * mix + b * (1f - mix);
    }

    override fun update(mix: Float) {
        bounds.minX = mix(boundsA.minX, boundsB.minX, mix)
        bounds.minY = mix(boundsA.minY, boundsB.minY, mix)
        bounds.minZ = mix(boundsA.minZ, boundsB.minZ, mix)
        bounds.maxX = mix(boundsA.maxX, boundsB.maxX, mix)
        bounds.maxY = mix(boundsA.maxY, boundsB.maxY, mix)
        bounds.maxZ = mix(boundsA.maxZ, boundsB.maxZ, mix)
    }

}

/**
 * Interpolates all BoundingBox dimensions linear and adds an optional parabolic component to it.
 */
class ParabolicBoundsInterpolator(boundsA: BoundingBox, boundsB: BoundingBox) :
        LinearBoundsInterpolator(boundsA, boundsB) {

    constructor(cfg: LayoutConfig) : this(cfg.getLayout(Orientation.PORTRAIT).bounds,
            cfg.getLayout(Orientation.LANDSCAPE).bounds)

    var amplitudeX = 0f
    var amplitudeY = 0f
    var amplitudeZ = 0f

    fun setAmplitudes(x: Float, y: Float, z: Float) {
        amplitudeX = x;
        amplitudeY = y;
        amplitudeZ = z;
    }

    override fun update(mix: Float) {
        super.update(mix)

        val f = 1f - (mix - 0.5f) * (mix - 0.5f) * 4f
        bounds.minX += f * amplitudeX
        bounds.maxX += f * amplitudeX
        bounds.minY += f * amplitudeY
        bounds.maxY += f * amplitudeY
        bounds.minZ += f * amplitudeZ
        bounds.maxZ += f * amplitudeZ
    }

}
