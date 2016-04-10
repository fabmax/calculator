package de.fabmax.calc.ui

import android.content.Context
import android.opengl.Matrix
import android.util.Log
import de.fabmax.calc.FloatAnimation
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlFont
import de.fabmax.lightgl.util.GlMath
import de.fabmax.lightgl.util.Painter

/**
 * A clickable button
 */
class Button(context: Context) : Panel<ButtonConfig>(ButtonConfig(), context) {

    var fontColor: Color = Color.BLACK
    var fontConfig: GlFont.FontConfig? = null
    var text: String = ""

    private var mFont: GlFont? = null

    private var mPressX = 0f
    private var mPressY = 0f
    private var mElevated = false
    private var mDrawElevated = false

    private val mPressAnimAlpha = FloatAnimation()
    private val mPressAnimSize = FloatAnimation()
    private val mFlipAnim = FloatAnimation()

    override fun paint(painter: Painter) {
        val c = layoutConfig.color

        val flipAng = mFlipAnim.animate()
        if (flipAng > .001f) {
            setupFlip(painter, flipAng, c)
        }

        // increase brightness depending on z
        var b = 1 + GlMath.clamp(z / dp(800f, context), -.2f, .2f);
        painter.setColor(c[0]*b, c[1]*b, c[2]*b, c[3])
        painter.fillRect(0.0f, 0.0f, width, height)

        if (mDrawElevated) {
            // draw another rect for elevated button
            b = 1 + GlMath.clamp((z + 16f) / dp(800f, context), -.2f, .2f);
            painter.setColor(c[0]*b, c[1]*b, c[2]*b, c[3])
            painter.translate(0f, 0f, dp(-16f, context))
            painter.fillRect(0.0f, 0.0f, width, height)
            painter.translate(0f, 0f, dp(16f, context))
        }

        if (!painter.glContext.state.isPrePass) {
            // text is only drawn if not in depth pass
            if (!text.isEmpty()) {
                if (fontConfig != null) {
                    mFont = GlFont.createFont(painter.glContext, fontConfig, fontColor)
                    fontConfig = null
                }

                mFont?.setScale(layoutConfig.textSize)
                painter.font = mFont
                val x = (width - painter.font.getStringWidth(text)) / 2.0f
                val y = (height + painter.font.fontSize * 0.65f) / 2.0f
                painter.drawString(x, y, text)
            }

        }

        if (flipAng > .001f) {
            painter.commit()
            painter.glContext.state.popModelMatrix()
        }

        if (!painter.glContext.state.isPrePass && (pressed || !mPressAnimAlpha.isDone)) {
            painter.setColor(Color.YELLOW, mPressAnimAlpha.animate())
            painter.fillCircle(mPressX, mPressY, mPressAnimSize.animate())
        }
    }

    private fun setupFlip(painter: Painter, flipAng: Float, color: FloatArray) {
        painter.glContext.state.pushModelMatrix()
        Matrix.translateM(painter.glContext.state.modelMatrix, 0, width/2, 0f, 0f)
        Matrix.rotateM(painter.glContext.state.modelMatrix, 0, flipAng, 0f, 1f, 0f)
        Matrix.translateM(painter.glContext.state.modelMatrix, 0, -width/2, 0f, 0f)
        painter.glContext.state.matrixUpdate()

        val cr = .5f + Math.abs(Math.cos(Math.toRadians(flipAng.toDouble())).toFloat()) / 2
        color[0] *= cr
        color[1] *= cr
        color[2] *= cr
    }

    fun flipText(newText: String) {
        mFlipAnim.set(0f, 90f).start(.15f)
        mFlipAnim.whenDone = { ->
            text = newText
            mFlipAnim.set(270f, 360f).start(.15f)
            mFlipAnim.whenDone = { -> mFlipAnim.set(0f, 0f) }
        }
    }

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)

        if (mElevated && !mDrawElevated) {
            mDrawElevated = true
        } else if (!mElevated && mDrawElevated) {
            mDrawElevated = false
        }
    }

    override fun onPress(x: Float, y: Float) {
        super.onPress(x, y)
        mPressX = x
        mPressY = y

        // circle animation
        mPressAnimSize.set(dp(60f, context), dp(100f, context)).start(0.1f, true)
        mPressAnimAlpha.set(0f, 0.5f).start(0.1f, false)

        // elevate button
        if (!mElevated) {
            mElevated = true
            layoutConfig.translate(Orientation.ALL, 0f, 0f, dp(16f, context))
        }
    }

    override fun onRelease() {
        super.onRelease()

        // circle animation
        mPressAnimSize.change(dp(30f, context)).start(0.2f, true)
        mPressAnimAlpha.change(0f).start(0.2f, false)

        // de-elevate button
        if (mElevated) {
            mElevated = false
            layoutConfig.translate(Orientation.ALL, 0f, 0f, dp(-16f, context))
        }
    }
}

open class ButtonConfig : PanelConfig() {
    private val props = Array(Orientation.ALL, { i -> ButtonProperties() })

    var textSize = 1f

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)

        textSize = props[Orientation.PORTRAIT].textSize * portLandMix +
                props[Orientation.LANDSCAPE].textSize * (1f - portLandMix)
    }

    fun getButtonProperties(orientation: Int): ButtonProperties {
        return props[orientation]
    }

    fun setTextSize(orientation: Int, size: Float) {
        if (orientation >= 0 && orientation < props.size) {
            props[orientation].textSize = size
        } else {
            props.forEach { p -> p.textSize = size }
        }
    }
}

class ButtonProperties {
    var textSize = 1f
}

class ButtonBuilder(context: Context) : AbstractPanelBuilder<Button>(context) {
    var textSize: Float
        get() = element.layoutConfig.getButtonProperties(orientation).textSize
        set(value) {
            element.layoutConfig.setTextSize(orientation, value)
        }

    override fun create(): Button {
        return Button(context)
    }
}
