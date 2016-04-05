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

    companion object {
        val TIMES = "\u00D7"
        val SQRT = "\u221A"
        val PI = "\u03C0"
        val DIGITS = Array(10, { i -> ('0' + i).toString()})
        val OPERATORS = arrayOf("+", "-", TIMES, "/", "^")
        val PARENTHESIS = arrayOf("(", ")")
        val CONSTANTS = arrayOf(PI, "e")
        val FUNCTIONS = arrayOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", SQRT)
    }

    var fontColor: Color = Color.BLACK
    var fontConfig: GlFont.FontConfig? = null
    var font: GlFont? = null

    var expr: String = ""
        private set

    var result: String = ""
        private set

    private val evaluator = SqrtEnabledEvaluator.getInstance()

    object exprState {
        var number = ""
    }

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
        exprState.number = ""
        expr = ""
        result = ""
    }

    private fun delPressed() {
        if (!expr.isEmpty()) {
            if (!exprState.number.isEmpty()) {
                exprState.number = exprState.number.substring(0, exprState.number.lastIndex)
            }
            expr = expr.substring(0, expr.lastIndex)
            evaluate(false)
        }
    }

    private fun evaluate(explicit: Boolean) {
        var e = expr
        e = e.replace(TIMES, "*")

        val openP = e.count { c -> c == '(' }
        val closeP = e.count { c -> c == ')' }
        for (i in 1 .. openP - closeP) {
            e += ")"
        }
        for (i in 1 .. closeP - openP) {
            e = "(" + e
        }

        // todo: find items which need an operator in front and insert and implicit * if needed

        try {
            val res = evaluator.evaluate(e)
            result = BigDecimal(res).round(MathContext(15)).stripTrailingZeros().toString()
        } catch(e: Exception) {
            if (explicit) {
                result = "Error"
            } else {
                result = ""
            }
            Log.d("CalcPanel", "Error on expression evaluation: " + e.message)
        }
    }

    fun appendExpression(expr: String) {
        this.expr += expr
        evaluate(false)
    }

    fun buttonPressed(button: String) {
        when (button) {
            in DIGITS -> digitPressed(button)
            in OPERATORS -> operatorPressed(button)
            in CONSTANTS -> constantsPressed(button)
            in PARENTHESIS -> parenthesisPressed(button)
            in FUNCTIONS -> functionPressed(button)
            "." -> dotPressed(button)
            "DEL" -> delPressed()
            "CLR" -> clear()
            "=" -> evaluate(true)
        }
    }

    private fun digitPressed(digit: String) {
        exprState.number += digit
        appendExpression(digit)
    }

    private fun dotPressed(dot: String) {
        if (!exprState.number.contains('.')) {
            exprState.number += dot
            appendExpression(dot)
        }
    }

    private fun operatorPressed(operator: String) {
        if (!expr.isEmpty() && !expr.endsWith("(") && !expr.endsWith(SQRT)) {
            if (expr.substring(expr.lastIndex) in OPERATORS) {
                // remove existing operator (will be replaced by new one)
                expr = expr.substring(0, expr.lastIndex)
            }
            exprState.number = ""
            appendExpression(operator)
        }
    }

    private fun parenthesisPressed(paren: String) {
        exprState.number = ""
        appendExpression(paren)
    }

    private fun constantsPressed(constant: String) {
        exprState.number = ""
        appendExpression(constant)
    }

    private fun functionPressed(function: String) {
        var txt = function
        if (function != SQRT) {
            txt += "("
        }
        exprState.number = ""
        appendExpression(txt)
    }

}

class CalcPanelBuilder(context: Context) : AbstractPanelBuilder<CalcPanel>(context) {
    override fun create(): CalcPanel {
        return CalcPanel(context)
    }
}
