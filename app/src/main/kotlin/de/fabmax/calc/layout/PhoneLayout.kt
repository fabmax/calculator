package de.fabmax.calc.layout

import android.content.Context
import de.fabmax.calc.*
import de.fabmax.lightgl.util.CharMap
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlFont

/**
 * Phone layout definition
 */
fun phoneLayout(appContext: Context): Layout {
    //appContext.resources.getColor(R.color.lightGray, null)
    val lightGray = Color("#e1e7ea")
    val gray = Color("#78909c")
    val darkGray = Color("#536771")
    val red = Color("#ef5350")
    val blue = Color("#5c6bc0")
    val cyan = Color("#8cf2f2")
    val petrol = Color("#26c6da")

    val chars = CharMap(" 0123456789.=+-*/sincotaner()^vlg\u221A\u03C0CLRDEL")

    val fontCfg = GlFont.FontConfig(appContext.assets, "Roboto-Light.ttf", chars, dp(24f, appContext))
    val fontCfgLarge = GlFont.FontConfig(appContext.assets, "Roboto-Thin.ttf", chars, dp(40f, appContext))

    return layout(appContext) {
        port { bounds(rw(-0.5f), rh(-0.5f), rw(1.0f), rh(1.0f)) }
        land { bounds(rw(-0.5f), rh(-0.5f), rw(1.0f), rh(1.0f)) }

        val calcPanel = calcPanel {
            init {
                fontConfig = fontCfgLarge
                fontColor = darkGray
            }
            port {
                color = lightGray
                bounds(dp(0f), dp(0f), parentW(), rh(0.25f))
            }
            land {
                color = lightGray
                bounds(dp(0f), dp(0f), parentW(), rh(0.30f))
            }
        }

        var texts = arrayListOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "")
        for (i in 0 .. 11) {
            button {
                init {
                    color = darkGray
                    fontConfig = fontCfg
                    fontColor = lightGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.appendExpression(text) }
                }
                port { bounds(rw(1/4f * (i%3)), rh(1/4f + (i/3) * 0.15f), rw(1/4f), rh(0.15f)) }
                land { bounds(rw(1/8f * (i%3)), rh(0.3f + (i/3) * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }

        texts = arrayListOf("+", "-", "*", "/")
        for (i in 0 .. 3) {
            button {
                init {
                    color = petrol
                    fontConfig = fontCfg
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.appendExpression(text) }
                }
                port {
                    bounds(rw(0.75f), rh(1/4f + i * 0.15f), rw(1/4F), rh(0.15f))
                    color = cyan
                }
                land { bounds(rw(0.375f), rh(0.3f + i * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }

        texts = arrayListOf("CLR", "DEL", "", "=")
        for (i in 0 .. 3) {
            button {
                init {
                    color = petrol
                    fontConfig = fontCfg
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.clear() }

                    val interpolator = ParabolicBoundsInterpolator(layoutConfig)
                    interpolator.amplitudeZ = dp((4-i) * 50f, context)
                    layoutConfig.boundsInterpolator = interpolator
                }
                port {
                    bounds(rw(1/4f * i), rh(0.85f), rw(1/4f), rh(0.15f))
                    if (texts[i] == "="){
                        color = cyan
                    }
                }
                land { bounds(rw(0.5f), rh(0.3f + i * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }

        texts = arrayListOf("sin", "cos", "tan", "ln", "log", "inv", "\u221A", "\u03C0", "e", "^", "(", ")")
        for (i in 0 .. 11) {
            button {
                init {
                    color = cyan
                    fontConfig = fontCfg
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.appendExpression(text) }
                }
                port { bounds(rw(1.2f+i*0.1f + 1/4f * (i%3)), rh(1/4f + (i/3) * 0.15f), rw(1/4f), rh(0.15f)) }
                land { bounds(rw(0.625f + 1/8f * (i%3)), rh(0.3f + (i/3) * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }
    }
}