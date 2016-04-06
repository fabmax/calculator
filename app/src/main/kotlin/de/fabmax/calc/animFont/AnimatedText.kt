package de.fabmax.calc.animFont

import android.opengl.Matrix
import android.util.Log
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.Painter

class AnimatedText(maxChars: Int) {

    private val start: Array<AnimateableChar>
    private val dest: Array<AnimateableChar>
    private val chars: Array<MutableChar>
    private val animState: Array<AnimationState>
    private val dots: BooleanArray

    private val colors = arrayOf(Color("#ef5350"), Color("#5c6bc0"), Color("#8cf2f2"), Color("#26c6da"))
    private val gray = Color("#78909c")

    private val dotChar = AnimateableChar.getChar('.')

    init {
        start = Array(maxChars, { i -> AnimateableChar.NULL_CHAR })
        dest = Array(maxChars, { i -> AnimateableChar.NULL_CHAR })
        chars = Array(maxChars, { i -> MutableChar() })
        animState = Array(maxChars, { i -> AnimationState() })
        dots = BooleanArray(maxChars)
    }

    fun setText(text: String) {
        var s = text
        s = s.replace(',', '.')

        for (i in chars.indices) {
            dots[i] = false
        }

        var i = 0
        var j = 0
        while (i < chars.size) {
            start[i] = dest[i]
            if (j < s.length) {
                val c = s[j]
                if (c == '.') {
                    dots[i] = true
                    i--
                } else {
                    val cd = AnimateableChar.getChar(c)
                    if (cd !== dest[i]) {
                        dest[i] = cd
                        animState[i].activate()
                        animState[i].blend = AnimationState.BLEND_TIME
                    }
                }
            } else {
                dest[i] = AnimateableChar.NULL_CHAR
                if (start[i] != AnimateableChar.NULL_CHAR) {
                    animState[i].activate()
                    animState[i].blend = AnimationState.BLEND_TIME
                }
            }
            i++
            j++
        }
    }

    fun animate(dT: Float) {
        for (i in chars.indices) {
            animState[i].animate(dT)
            if (animState[i].blend > 0 || (start[i] != dest[i] && (start[i] != AnimateableChar.NULL_CHAR || dest[i] != AnimateableChar.NULL_CHAR))) {
                start[i].blend(dest[i], animState[i].blendSmooth, chars[i])
                if (animState[i].blend == 0f) {
                    start[i] = dest[i]
                }
            }
        }
    }

    fun draw(painter: Painter, size: Float, maxW: Float) {
        val scale = size / 2
        val ctx = painter.glContext
        painter.pushTransform()

        painter.scale(scale, -scale, scale);
        painter.setLineThickness(4f/scale)

        var w = 0f
        for (i in chars.indices) {
            if (start[i] !== AnimateableChar.NULL_CHAR || dest[i] !== AnimateableChar.NULL_CHAR) {
                w += chars[i].charAdvance
            }
        }
        Matrix.translateM(ctx.state.modelMatrix, 0, -w, 0f, 0f)
        ctx.state.matrixUpdate()
        w *= scale

        for (i in chars.indices) {
            if (start[i] === AnimateableChar.NULL_CHAR && dest[i] === AnimateableChar.NULL_CHAR) {
                break
            }

            Matrix.translateM(ctx.state.modelMatrix, 0, chars[i].charAdvance / 2, 0f, 0f)
            ctx.state.matrixUpdate()

            if (w < maxW) {
                if (animState[i].blendSmooth < 0.0001f && animState[i].animationTime <= 0) {
                    painter.setColor(gray)
                    dest[i].draw(painter)
                } else {
                    chars[i].draw(painter, gray, animState[i].offset, animState[i].secSize / 4, colors)
                }
                painter.commit()
                if (dots[i]) {
                    painter.setColor(gray)
                    dotChar.draw(painter)
                }
            }
            w -= chars[i].charAdvance * scale

            Matrix.translateM(ctx.state.modelMatrix, 0, chars[i].charAdvance / 2, 0f, 0f)
            ctx.state.matrixUpdate()
        }
        painter.popTransform()
    }

    private class AnimationState {

        companion object {
            val BLEND_TIME = 0.3f
        }

        var offset = Math.random().toFloat()
        var secSize = 0f
        var animationTime = 0f
        var blend = BLEND_TIME
        var blendSmooth = 1f

        fun activate() {
            animationTime = 4f;
            //animationTime = 4000f
        }

        fun animate(dt: Float) {
            if (blend > 0) {
                blend -= dt
                if (blend < 0) {
                    blend  = 0f
                }
                blendSmooth = (1 - Math.cos(blend / BLEND_TIME * Math.PI)).toFloat() / 2
            }

            if (animationTime > 0) {
                offset += dt / 3
                if (animationTime < 1) {
                    offset += dt / 2
                }
                offset %= 1
                animationTime -= dt

                if (secSize > animationTime) {
                    secSize = animationTime
                    if (secSize < 0) {
                        secSize = 0f
                    }

                } else if (secSize < 1) {
                    secSize += dt
                    if (secSize > 1) {
                        secSize = 1f
                    }
                }
            }
        }
    }
}
