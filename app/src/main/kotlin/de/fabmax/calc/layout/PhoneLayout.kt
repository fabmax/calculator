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
    val lightGray = Color("#e1e7ea")
    val gray = Color("#78909c")
    val darkGray = Color("#536771")
    val red = Color("#ef5350")
    val blue = Color("#5c6bc0")
    val cyan = Color("#8cf2f2")
    val petrol = Color("#26c6da")

    val chars = CharMap(" 0123456789.=+-/sincotaner()^vlgCLRDEL" + CalcPanel.TIMES + CalcPanel.PI + CalcPanel.SQRT)

    val fontCfg = GlFont.FontConfig(appContext.assets, "Roboto-Light.ttf", chars, dp(28f, appContext))
    val fontCfgSmall = GlFont.FontConfig(appContext.assets, "Roboto-Light.ttf", chars, dp(18f, appContext))
    val fontCfgLarge = GlFont.FontConfig(appContext.assets, "Roboto-Thin.ttf", chars, dp(40f, appContext))

    return layout(appContext) {
        port { bounds(rw(-0.5f), rh(-0.5f), rw(1.0f), rh(1.0f)) }
        land { bounds(rw(-0.5f), rh(-0.5f), rw(1.0f), rh(1.0f)) }

        val calcPanel = calcPanel {
            init {
                color = lightGray
            }
            port {
                bounds(dp(0f), dp(0f), dp(16f), rw(1.02f), rh(0.26f), dp(0f))
                textSize = dp(40f, context)
            }
            land {
                bounds(dp(0f), dp(0f), dp(16f), rw(1.03f), rh(0.31f), dp(0f))
                textSize = dp(30f, context)
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
                    onClickListener = { -> calcPanel.buttonPressed(text) }
                }
                port { bounds(rw(1 / 4f * (i % 3)), rh(1 / 4f + (i / 3) * 0.15f), rw(1 / 4f), rh(0.15f)) }
                land { bounds(rw(1/8f * (i%3)), rh(0.3f + (i/3) * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }

        texts = arrayListOf("+", "-", CalcPanel.TIMES.toString(), "/")
        for (i in 0 .. 3) {
            button {
                init {
                    fontConfig = fontCfgSmall
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.buttonPressed(text) }
                }
                port {
                    bounds(rw(0.74f), rh(1/4f + i * 0.15f), dp(8f), rw(.26f), rh(0.15f), dp(0f))
                    color = cyan
                }
                land {
                    bounds(rw(0.375f), rh(0.3f + i * 0.175f), dp(8f), rw(1/8f), rh(0.175f), dp(0f))
                    color = petrol
                }
            }
        }

        texts = arrayListOf("CLR", "DEL", "", "=")
        for (i in 0 .. 3) {
            button {
                init {
                    color = petrol
                    fontConfig = fontCfgSmall
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.buttonPressed(text) }

                    val interpolator = ParabolicBoundsInterpolator(layoutConfig)
                    interpolator.amplitudeZ = dp((4-i) * 50f, context)
                    layoutConfig.boundsInterpolator = interpolator
                }
                port {
                    bounds(rw(.26f * i - .01f), rh(0.85f), dp(-16f), rw(.26f), rh(0.16f), dp(0f))
                    if (texts[i] == "="){
                        bounds(rw(.74f), rh(0.85f), dp(8f), rw(.26f), rh(0.15f), dp(0f))
                        color = cyan
                    }
                }
                land { bounds(rw(.5f), rh(.3f + i * .175f), dp(8f), rw(1/8f), rh(0.175f), dp(0f)) }
            }
        }

        texts = arrayListOf("sin", "cos", "tan", "ln", "log", "inv", CalcPanel.SQRT.toString(),
                CalcPanel.PI.toString(), "e", "^", "(", ")")
        for (i in 0 .. 11) {
            button {
                init {
                    color = cyan
                    fontConfig = fontCfgSmall
                    fontColor = darkGray
                    text = texts[i]
                    onClickListener = { -> calcPanel.buttonPressed(text) }
                }
                port {
                    bounds(rw(1.4f + 1/8f * (i%3) + i/3*.25f), rh(1/4f + (i/3) * 0.15f),
                            dp((i/3+1) * -50f), rw(1/4f), rh(0.15f), dp(0f))
                }
                land { bounds(rw(0.625f + 1/8f * (i%3)), rh(0.3f + (i/3) * 0.175f), rw(1/8f), rh(0.175f)) }
            }
        }
    }
}