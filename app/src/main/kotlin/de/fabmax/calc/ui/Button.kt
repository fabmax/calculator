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
class Button(context: Context) : Panel<PanelConfig>(PanelConfig(), context) {

    var fontColor: Color = Color.BLACK
    var fontConfig: GlFont.FontConfig? = null
    var font: GlFont? = null
    var text: String = ""

    private var mPressX = 0f
    private var mPressY = 0f
    private var mElevated = false

    private val mPressAnimAlpha = FloatAnimation()
    private val mPressAnimSize = FloatAnimation()

//    private var ang = 0f

    override fun paint(painter: Painter) {
        val c = layoutConfig.color

        // increase brightness depending on z
        var b = 1 + GlMath.clamp(z / dp(800f, context), -.2f, .2f);
        painter.setColor(c[0]*b, c[1]*b, c[2]*b, c[3])
        painter.fillRect(0.0f, 0.0f, width, height)

        if (mElevated) {
            // draw another rect for elevated button
            b = 1 + GlMath.clamp((z + 16f) / dp(800f, context), -.2f, .2f);
            painter.setColor(c[0]*b, c[1]*b, c[2]*b, c[3])
            painter.translate(0f, 0f, dp(16f, context))
            painter.fillRect(0.0f, 0.0f, width, height)
        }

        if (painter.glContext.state.isPrePass) {
            // on shadow render pass, quit after background planes are drawn
            return
        }

        if (!text.isEmpty()) {
            if (fontConfig != null) {
                font = GlFont.createFont(painter.glContext, fontConfig, fontColor)
                fontConfig = null
            }
            if (font != null) {
                painter.font = font
            }

            val x = (width - painter.font.getStringWidth(text)) / 2.0f
            val y = (height + painter.font.fontSize * 0.65f) / 2.0f
            painter.drawString(x, y, text)
        }

        if (pressed || !mPressAnimAlpha.isDone) {
            painter.setColor(Color.YELLOW, mPressAnimAlpha.animate())
            painter.fillCircle(mPressX, mPressY, mPressAnimSize.animate())
        }
    }

    override fun onPress(x: Float, y: Float) {
        super.onPress(x, y)
        mPressX = x
        mPressY = y

        // circle animation
        mPressAnimSize.start(dp(60f, context), dp(100f, context)).overTime(0.1f, true)
        mPressAnimAlpha.start(0f, 0.5f).overTime(0.1f, false)

        // elevate button
        if (!mElevated) {
            mElevated = true
            layoutConfig.getLayout(Orientation.PORTRAIT).bounds.maxZ += 16f
            layoutConfig.getLayout(Orientation.LANDSCAPE).bounds.maxZ += 16f
            //layoutConfig.translate(Orientation.ALL, 0f, 0f, dp(16f, context))
        }
    }

    override fun onRelease() {
        super.onRelease()

        // circle animation
        mPressAnimSize.change(dp(30f, context)).overTime(0.2f, true)
        mPressAnimAlpha.change(0f).overTime(0.2f, false)

        // de-elevate button
        if (mElevated) {
            mElevated = false
            layoutConfig.getLayout(Orientation.PORTRAIT).bounds.maxZ -= 16f
            layoutConfig.getLayout(Orientation.LANDSCAPE).bounds.maxZ -= 16f
            //layoutConfig.translate(Orientation.ALL, 0f, 0f, dp(-16f, context))
        }
    }
}

/*class ButtonConfig : LayoutConfig() {

    private val props = Array(Orientation.ALL, { i -> ButtonProperties() })

    val color = FloatArray(4)

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)

        val colorP = props[Orientation.PORTRAIT].color
        val colorL = props[Orientation.LANDSCAPE].color
        color[0] = colorP.r * portLandMix + colorL.r * (1f - portLandMix)
        color[1] = colorP.g * portLandMix + colorL.g * (1f - portLandMix)
        color[2] = colorP.b * portLandMix + colorL.b * (1f - portLandMix)
        color[3] = colorP.a * portLandMix + colorL.a * (1f - portLandMix)
    }

    fun getButtonProperties(orientation: Int): ButtonProperties {
        return props[orientation]
    }

    fun setColor(orientation: Int, color: Color) {
        if (orientation >= 0 && orientation < props.size) {
            props[orientation].color = color
        } else {
            props.forEach { p -> p.color = color }
        }
    }
}

class ButtonProperties {

    var color = Color.LIGHT_GRAY

}*/

class ButtonBuilder(context: Context) : AbstractPanelBuilder<Button>(context) {
    override fun create(): Button {
        return Button(context)
    }
}
