package de.fabmax.calc

import android.content.Context
import android.util.Log
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlFont
import de.fabmax.lightgl.util.Painter
import java.math.BigDecimal
import java.math.MathContext

/**
 * The panel used to display the current expression and result
 */
class CalcPanel(context: Context) : Panel<PanelConfig>(PanelConfig(), context) {

    var fontColor: Color = Color.BLACK
    var fontConfig: GlFont.FontConfig? = null
    var font: GlFont? = null

    var expr: String = ""
        private set

    var result: String = ""
        private set

    private val evaluator = SqrtEnabledEvaluator.getInstance()

    override fun paint(painter: Painter) {
        val color = layoutConfig.color
        painter.setColor(color)
        painter.fillRect(0.0f, 0.0f, width, height)

        if (fontConfig != null) {
            font = GlFont.createFont(painter.glContext, fontConfig, fontColor)
            fontConfig = null
        }
        if (font != null) {
            painter.font = font
        }

        val y = (height + painter.font.fontSize * 0.65f) / 2.0f
        var x1 = (width - painter.font.getStringWidth(expr) - dp(16f, context))
        var x2 = (width - painter.font.getStringWidth(result) - dp(16f, context))
        painter.drawString(x1, y - painter.font.lineSpace / 2, expr)
        painter.drawString(x2, y + painter.font.lineSpace / 2, result)
    }

    fun clear() {
        expr = ""
        result = ""
    }

    fun appendExpression(expr: String) {
        this.expr += expr
        try {
            val res = evaluator.evaluate(this.expr)
            result = BigDecimal(res).round(MathContext(15)).stripTrailingZeros().toString()
        } catch(e: Exception) {
            result = "Error"
            Log.d("CalcPanel", "Error on expression evaluation: " + e.message)
        }
    }
}

class CalcPanelBuilder(context: Context) : AbstractPanelBuilder<CalcPanel>(context) {
    override fun create(): CalcPanel {
        return CalcPanel(context)
    }
}
