package de.fabmax.calc.ui

import android.content.Context
import android.util.Log
import de.fabmax.calc.ui.SqrtEnabledEvaluator
import de.fabmax.calc.animFont.AnimateableChar
import de.fabmax.calc.animFont.AnimatedText
import de.fabmax.lightgl.BoundingBox
import de.fabmax.lightgl.scene.DynamicLineMesh
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlFont
import de.fabmax.lightgl.util.Painter
import java.math.BigDecimal
import java.math.MathContext

/**
 * The panel used to display the current expression and result
 */
class CalcPanel(context: Context) : Panel<CalcPanelConfig>(CalcPanelConfig(), context) {

    companion object {
        val TIMES = '\u00D7'
        val SQRT = '\u221A'
        val PI = '\u03C0'
        val DIGITS = Array(10, { i -> ('0' + i).toString()})
        val OPERATORS = arrayOf("+", "-", TIMES.toString(), "/", "^")
        val PARENTHESIS = arrayOf("(", ")")
        val CONSTANTS = arrayOf(PI.toString(), "e")
        val FUNCTIONS = arrayOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", SQRT.toString())
    }

    var expr: String = ""
        private set

    var time = System.currentTimeMillis()

    private val evaluator = SqrtEnabledEvaluator.getInstance()
    private val animExpr = AnimatedText(32)
    private val animResult = AnimatedText(32)

    object exprState {
        var number = ""
    }

    override fun paint(painter: Painter) {
        val color = layoutConfig.color
        painter.setColor(color)
        painter.fillRect(0.0f, 0.0f, width, height)

        if (painter.glContext.state.isPrePass) {
            return
        }

        val t = System.currentTimeMillis()
        val dt = (t - time) / 1000f
        time = t
        animExpr.animate(dt)
        animResult.animate(dt)

        val margin = dp(32f, context)
        val textSz = layoutConfig.textSize

        painter.pushTransform()
        painter.translate(width - margin, height/2 - textSz * .75f, 0f)
        animExpr.draw(painter, textSz, width)
        painter.translate(0f, textSz * 1.5f, 0f)
        animResult.draw(painter, textSz, width)

        painter.popTransform()
    }

    fun clear() {
        exprState.number = ""
        expr = ""
        animExpr.setText(expr)
        animResult.setText("")
    }

    private fun delPressed() {
        if (!expr.isEmpty()) {
            if (!exprState.number.isEmpty()) {
                exprState.number = exprState.number.substring(0, exprState.number.lastIndex)
            }
            expr = expr.substring(0, expr.lastIndex)
            evaluate(false)
            animExpr.setText(this.expr)
        }
    }

    private fun evaluate(explicit: Boolean) {
        var e = expr
        e = e.replace(TIMES, '*')

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
            Log.d("CalcPanel", "Evaluating: " + e)
            val res = evaluator.evaluate(e)
            var resStr = BigDecimal(res).round(MathContext(15)).toString()
            Log.d("CalcPanel", "Result: " + resStr)

            // unfortunately BigDecimal.stripTrailingZeros() also removes 0 before the decimal point
            // so 10 becomes 1e1 which is ugly...
            if (resStr.contains('.')) {
                while (resStr.endsWith('0') || resStr.endsWith('.')) {
                    resStr = resStr.substring(0, resStr.lastIndex)
                }
            }

            if (resStr != e) {
                if (explicit) {
                    this.expr = resStr
                    exprState.number = resStr
                    animExpr.setText(resStr)
                    animResult.setText("")
                } else {
                    animResult.setText(resStr)
                }
            } else {
                animResult.setText("")
            }
        } catch(e: Exception) {
            if (explicit) {
                animResult.setText("error")
            } else {
                animResult.setText("")
            }
            Log.d("CalcPanel", "Error on expression evaluation: " + e.message)
        }
    }

    fun appendExpression(expr: String) {
        this.expr += expr
        evaluate(false)
        animExpr.setText(this.expr)
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
        } else if (operator == "-") {
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
        if (function != SQRT.toString()) {
            txt += "("
        }
        exprState.number = ""
        appendExpression(txt)
    }

}

open class CalcPanelConfig : PanelConfig() {
    private val props = Array(Orientation.ALL, { i -> CalcPanelProperties() })

    var textSize = 0f

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)

        textSize = props[Orientation.PORTRAIT].textSize * portLandMix +
                props[Orientation.LANDSCAPE].textSize * (1f- portLandMix)
    }

    fun getCalcPanelProperties(orientation: Int): CalcPanelProperties {
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

class CalcPanelProperties {
    var textSize = 100f
}

class CalcPanelBuilder(context: Context) : AbstractPanelBuilder<CalcPanel>(context) {
    var textSize: Float
        get() = element.layoutConfig.getCalcPanelProperties(orientation).textSize
        set(value) {
            element.layoutConfig.setTextSize(orientation, value)
        }

    override fun create(): CalcPanel {
        return CalcPanel(context)
    }
}
