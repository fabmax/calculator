package de.fabmax.calc.ui

import android.content.Context
import android.util.Log
import de.fabmax.calc.FloatAnimation
import de.fabmax.calc.SqrtEnabledEvaluator
import de.fabmax.calc.animFont.AnimatedText
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlFont
import de.fabmax.lightgl.util.GlMath
import de.fabmax.lightgl.util.Painter
import java.math.BigDecimal
import java.math.MathContext

/**
 * The panel used to display the current expression and result.
 */
class CalcPanel(context: Context) : Panel<CalcPanelConfig>(CalcPanelConfig(), context) {

    companion object {
        val TIMES = '\u00D7'
        val DIVISION = '\u00F7'
        val SQRT = '\u221A'
        val PI = '\u03C0'
        val DIGITS = Array(10, { i -> ('0' + i).toString()})
        val OPERATORS = arrayOf("+", "-", TIMES.toString(), DIVISION.toString(), "^")
        val PARENTHESIS = arrayOf("(", ")")
        val CONSTANTS = arrayOf(PI.toString(), "e")
        val FUNCTIONS = arrayOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", SQRT.toString())

        val T_IO = 1463589000000L - 1
    }

    var fontColor: Color = Color.BLACK
    var fontConfig: GlFont.FontConfig? = null

    var expr: String = ""
        private set
    var result: String = ""
        private set

    private var mFont: GlFont? = null
    private var mErrorFont: GlFont? = null
    private val mErrorColor = Color("#EA004A")

    private var mIoMode = false
    private var mTime = System.currentTimeMillis()
    private var mClearing = false

    private val mResultAnimation = FloatAnimation()
    private val mClearAnimation = FloatAnimation()
    private val mErrorAnimation = FloatAnimation()

    private val mEvaluator = SqrtEnabledEvaluator.getInstance()
    private val mAnimText1 = AnimatedText(32)
    private val mAnimText2 = AnimatedText(32)

    private var mNumber = ""

    init {
        mClearAnimation.set(1f, 0f)
        mClearAnimation.whenDone = { ->
            expr = ""
            result = ""
            mNumber = ""
            mClearing = false
            mIoMode = false
            mClearAnimation.set(1f, 0f)
        }
    }

    override fun paint(painter: Painter) {
        val color = layoutConfig.color
        painter.setColor(color)
        painter.fillRect(0.0f, 0.0f, width, height)

        if (painter.glContext.state.isPrePass) {
            return
        }

        if (fontConfig != null) {
            mFont = GlFont.createFont(painter.glContext, fontConfig, fontColor)
            mErrorFont = GlFont.createFont(painter.glContext, fontConfig, mErrorColor)
            fontConfig = null
        }

        if (!mIoMode) {
            drawTexts(painter)
            if (expr == "1" + DIVISION + "0") {
                drawIoIndicator(painter)
            }
        } else {
            mClearAnimation.animate()
            drawIoCountdown(painter)
        }

        val errorA = mErrorAnimation.animate()
        if (errorA > .001f) {
            painter.commit()
            painter.setColor(mErrorColor, errorA)
            painter.fillRect(0.0f, 0.0f, width, height)
        }
    }

    private fun drawTexts(painter: Painter) {
        val clearA = mClearAnimation.animate()
        val resultA = mResultAnimation.animate()

        mFont?.setScale(layoutConfig.textSize * clearA)
        painter.font = mFont

        val margin = dp(32f, context)
        val y = (height + painter.font.fontSize * .85f) / 2.0f
        val y1 = y - painter.font.lineSpace * .6f
        val y2 = y + painter.font.lineSpace * .6f * .8f
        val dy = resultA * (y1 - y2)

        val x1 = (width - painter.font.getStringWidth(expr) - margin)
        painter.setAlpha(1f - GlMath.clamp(resultA * 2, 0f, 1f))
        painter.drawString(x1, y1 + dy, expr)
        painter.setAlpha(1f)

        var font = mFont!!
        if (result == "error") {
            font = mErrorFont!!
            painter.font = font
        }
        font.setScale(layoutConfig.textSize * (.8f + .2f * resultA) * clearA)
        val x2 = (width - font.getStringWidth(result) - margin)
        painter.drawString(x2, y2 + dy, result)
    }

    private fun drawIoIndicator(painter: Painter) {
        animateIo()
        mAnimText2.text = "1/0 2016"

        val margin = dp(32f, context)
        val textSz = dp(34f * layoutConfig.textSize, context)

        painter.pushTransform()
        painter.translate(width - margin, height/2 + textSz * .75f, 0f)
        mAnimText2.draw(painter, textSz, width)
        painter.popTransform()
    }

    private fun drawIoCountdown(painter: Painter) {
        animateIo()
        if (!mClearing) {
            var t = (T_IO - System.currentTimeMillis()) / 1000
            if (t >= 0) {
                mAnimText2.text = String.format("%02d, %02d, %02d, %02d",
                        t / 86400, (t % 86400) / 3600, (t % 3600) / 60, t % 60)
            } else {
                t = -t
                mAnimText2.text = String.format("-%02d, %02d, %02d, %02d",
                        t / 86400, (t % 86400) / 3600, (t % 3600) / 60, t % 60)
            }
            mAnimText1.text = "1/0 2016"
        }

        val margin = dp(32f, context)
        val textSz = dp(30f * layoutConfig.textSize, context)

        painter.pushTransform()
        painter.translate(width - margin, height/2 - textSz * 0.9f, 0f)
        mAnimText1.draw(painter, textSz, width)
        painter.translate(0f, textSz * 1.8f, 0f)
        mAnimText2.draw(painter, textSz, width)
        painter.popTransform()
    }

    private fun animateIo() {
        val t = System.currentTimeMillis()
        val dt = (t - mTime) / 1000f
        mTime = t
        mAnimText1.animate(dt)
        mAnimText2.animate(dt)
    }

    fun clear() {
        mClearing = true
        mAnimText1.text = ""
        mAnimText2.text = ""
        mClearAnimation.start(0.25f, true)
    }

    private fun evaluate(explicit: Boolean) {
        var e = expr.replace(TIMES, '*')
        e = e.replace(DIVISION, '/')

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
            val res = mEvaluator.evaluate(e)
            var resStr = BigDecimal(res).round(MathContext(12)).toString()
            resStr = resStr.replace('E', 'e')
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
                    mNumber = resStr
                    mResultAnimation.whenDone = { ->
                        expr = result
                        result = ""
                        mResultAnimation.set(0f, 0f)
                    }
                    mResultAnimation.set(0f, 1f).start(.5f, true)
                } else {
                    result = resStr
                }
            } else {
                result = ""
            }
        } catch(e: Exception) {
            if (explicit) {
                if (expr == "1" + DIVISION + "0") {
                    mIoMode = true
                } else {
                    result = "error"
                    mErrorAnimation.set(0f, 1f).start(.05f)
                    mErrorAnimation.whenDone = { ->
                        mErrorAnimation.reverse().start(.4f)
                        mErrorAnimation.whenDone = null
                    }
                }
            } else {
                result = ""
            }
            Log.d("CalcPanel", "Error on expression evaluation: " + e.message)
        }
    }

    fun appendExpression(expr: String): Boolean {
        if (this.expr.length < 256) {
            this.expr += expr
            evaluate(false)
            return true
        }
        return false
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

    private fun delPressed() {
        if (!expr.isEmpty()) {
            if (!mNumber.isEmpty()) {
                mNumber = mNumber.substring(0, mNumber.lastIndex)
            }
            expr = expr.substring(0, expr.lastIndex)
            evaluate(false)
        }
    }

    private fun digitPressed(digit: String) {
        if (appendExpression(digit)) {
            mNumber += digit
        }
    }

    private fun dotPressed(dot: String) {
        if (!mNumber.contains('.') && appendExpression(dot)) {
            mNumber += dot
        }
    }

    private fun operatorPressed(operator: String) {
        if (!expr.isEmpty() && !expr.endsWith("(") && !expr.endsWith(SQRT)) {
            if (expr.substring(expr.lastIndex) in OPERATORS) {
                // remove existing operator (will be replaced by new one)
                expr = expr.substring(0, expr.lastIndex)
            }
            mNumber = ""
            appendExpression(operator)
        } else if (operator == "-") {
            mNumber = ""
            appendExpression(operator)
        }
    }

    private fun parenthesisPressed(paren: String) {
        mNumber = ""
        appendExpression(paren)
    }

    private fun constantsPressed(constant: String) {
        mNumber = ""
        appendExpression(constant)
    }

    private fun functionPressed(function: String) {
        var txt = function
        if (function != SQRT.toString()) {
            txt += "("
        }
        mNumber = ""
        appendExpression(txt)
    }

}

open class CalcPanelConfig : PanelConfig() {
    private val props = Array(Orientation.ALL, { i -> CalcPanelProperties() })

    var textSize = 1f

    override fun mixConfigs(portLandMix: Float) {
        super.mixConfigs(portLandMix)

        textSize = props[Orientation.PORTRAIT].textSize * portLandMix +
                props[Orientation.LANDSCAPE].textSize * (1f - portLandMix)
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
    var textSize = 1f
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
