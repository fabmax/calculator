package de.fabmax.calc.animFont

import android.util.Log
import java.util.HashMap

import de.fabmax.lightgl.util.*

open class AnimateableChar {

    val name: String

    protected val path: FloatArray
    protected val stepLens: FloatArray

    protected var isStatic = true
    private var mesh: MeshData? = null

    protected var pathLength: Float = 0.toFloat()
    open var charAdvance: Float = 0.toFloat()
        protected set

    private val tmpVert = FloatArray(3)

    protected constructor(name: String, nVerts: Int) {
        path = FloatArray(nVerts * 3)
        stepLens = FloatArray(nVerts)
        pathLength = 0f
        charAdvance = 0f
        this.name = name
    }

    constructor(name: String, pathDef: FloatList, charWidth: Float) {
        path = pathDef.asArray()
        stepLens = FloatArray(path.size / 3)
        charAdvance = charWidth
        this.name = name
        updatePathLength()
    }

    protected fun updatePathLength() {
        pathLength = 0f
        var x0 = path[0]
        var y0 = path[1]
        var z0 = path[2]
        var i = 3
        var j = 0
        while (i < path.size) {
            val x1 = path[i]
            val y1 = path[i + 1]
            val z1 = path[i + 2]

            stepLens[j] = Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1) + (z0 - z1) * (z0 - z1).toDouble()).toFloat()
            pathLength += stepLens[j]
            x0 = x1
            y0 = y1
            z0 = z1
            i += 3
            j++
        }
    }

    fun draw(painter: Painter) {
        if (isStatic && mesh != null && !mesh!!.isEmpty) {
            painter.drawMeshData(mesh)
        } else {
            if (mesh == null) {
                painter.commit()
            }
            var i = 0
            while (i < path.size - 3) {
                painter.drawLine(path[i], path[i + 1], path[i + 3], path[i + 4])
                i += 3
            }
            if (mesh == null) {
                mesh = painter.meshBuilder.build()
            }
        }
    }

    fun draw(painter: Painter, defaultColor: Color, offset: Float, secLen: Float, secColors: Array<Color>) {
        var color = defaultColor
        var remaining = offset
        var secIdx = -1

        // determine section (color) to start with
        for (i in secColors.indices) {
            val l = offset + secLen * (i + 1)
            if (l > 1) {
                secIdx = i
                color = secColors[secIdx]
                remaining = l - 1
                break
            }
        }
        var firstSi = secIdx
        remaining *= pathLength

        tmpVert[0] = path[0]
        tmpVert[1] = path[1]
        tmpVert[2] = path[2]

        // draw path by sections with varying colors
        painter.setColor(color)
        var stepLen = stepLens[0]
        //target.addVertex(path[0], path[1], path[2], color)
        var i = 3
        while (i < path.size) {
            if (remaining < stepLen) {
                // section (color) change
                color = defaultColor
                var nextLen = pathLength
                secIdx++

                if (secIdx >= secColors.size && firstSi != -1) {
                    // section overflow (we did not start with the first section)
                    firstSi = -1
                    secIdx = -1
                    nextLen = (1 - secColors.size * secLen) * pathLength
                } else if (secIdx < secColors.size) {
                    // select next section
                    color = secColors[secIdx]
                    nextLen = secLen * pathLength
                }

                // insert a vertex at the exact section end position
                split(painter, tmpVert, i, remaining / stepLen)
                painter.setColor(color)

                // compute remaining length of this step
                val dx = path[i] - tmpVert[0]
                val dy = path[i + 1] - tmpVert[1]
                val dz = path[i + 2] - tmpVert[2]
                stepLen = Math.sqrt(dx * dx + dy * dy + dz * dz.toDouble()).toFloat()

                // set next section values
                remaining = nextLen
                // we inserted a vertex, next path vertex remains the same
                i -= 3

            } else {
                // extend path with current color
                //target.extendLine(path[i], path[i + 1], path[i + 2], color)
                painter.drawLine(tmpVert[0], tmpVert[1], path[i], path[i+1])
                tmpVert[0] = path[i]
                tmpVert[1] = path[i + 1]
                tmpVert[2] = path[i + 2]
                remaining -= stepLen
                stepLen = stepLens[i / 3]
            }
            i += 3
        }
    }

    fun blend(other: AnimateableChar, weight: Float, target: MutableChar) {
        val w = weight
        val trgt: AnimateableChar = target
        if (w > 0.9999f) {
            target.setPath(this)
            target.setCharWidth(charAdvance)
        } else if (weight < 0.0001f) {
            target.setPath(other)
            target.setCharWidth(other.charAdvance)
        } else {
            for (i in path.indices) {
                trgt.path[i] = path[i] * w + other.path[i] * (1 - w)
            }
            target.setCharWidth(charAdvance * w + other.charAdvance * (1 - w))
        }
        trgt.updatePathLength()
    }

    private fun split(painter: Painter, vert: FloatArray, iNext: Int, p: Float) {
        var p = p
        if (p < 0 || p > 1) {
            System.err.println(p)
        }
        p = GlMath.clamp(p, 0f, 1f)

        val x0 = vert[0]
        val y0 = vert[1]
        val z0 = vert[2]
        val x2 = path[iNext]
        val y2 = path[iNext + 1]
        val z2 = path[iNext + 2]

        vert[0] = x0 * (1 - p) + x2 * p
        vert[1] = y0 * (1 - p) + y2 * p
        vert[2] = z0 * (1 - p) + z2 * p

        painter.drawLine(x0, y0, vert[0], vert[1])
    }

    companion object {

        val N_VERTS = 60

        val NULL_CHAR = makeNull()

        val TIMES = '\u00D7'
        val SQRT = '\u221A'
        val PI = '\u03C0'

        private val CHARS = object : HashMap<Char, AnimateableChar>() {
            init {
                put('0', make0())
                put('1', make1())
                put('2', make2())
                put('3', make3())
                put('4', make4())
                put('5', make5())
                put('6', make6())
                put('7', make7())
                put('8', make8())
                put('9', make9())
                put('.', makeComma())
                put('+', makePlus())
                put('-', makeMinus())
                put(TIMES, makeTimes())
                put('/', makeSlash())
                put('^', makePow())
                put('(', makeParenOpen())
                put(')', makeParenClose())
                put('a', make_a())
                put('c', makeC(true))
                put('e', make_e())
                put('g', make_g())
                put('i', makeI(true))
                put('l', make_l())
                put('n', make_n())
                put('o', makeO(true))
                put('r', make_r())
                put('s', makeS(true))
                put('t', make_t())
                put(PI, makePi())
                put(SQRT, makeSqrt())
            }
        }

        fun getChar(c: Char): AnimateableChar {
            val key = c
            if (CHARS.containsKey(key)) {
                return CHARS[c]!!
            } else {
                Log.e("AnimChar", "Char not mapped: " + c)
                return NULL_CHAR
            }
        }

        private fun makeNull(): AnimateableChar {
            val coords = FloatList()
            for (i in 0..N_VERTS * 3 - 1) {
                coords.add(0f)
            }
            return AnimateableChar("null", coords, 0f)
        }

        private fun make0(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 0f, 0f, 1f, -90.0, 360.0, N_VERTS)
            return AnimateableChar("0", coords, 2.3f)
        }

        private fun make1(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.2f, -1f, .2f, -1f, N_VERTS / 6)
            line(coords, .2f, -1f, .2f, 1f, N_VERTS / 3)
            line(coords, .2f, 1f, -.2f, 1f, N_VERTS / 3)
            line(coords, -.2f, 1f, -.2f, -1f, N_VERTS / 6)
            return AnimateableChar("1", coords, .85f)
        }

        private fun make2(): AnimateableChar {
            val coords = FloatList()
            line(coords, 0f, -1f, .6f, -1f, 4)
            line(coords, .6f, -1f, .4f, -.8f, 2)
            line(coords, .4f, -.8f, -.6f, -.8f, 8)
            sweep(coords, -0.12f, 0.48f, .52f, -35.0, 190.0, N_VERTS / 2 - 9)
            sweep(coords, 0.08f, 0.28f, .52f, 155.0, -190.0, N_VERTS / 2 - 9)
            line(coords, -.4f, -1f, -0f, -1f, 4)
            return AnimateableChar("2", coords, 1.65f)
        }

        private fun make3(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, -0.12f, -0.3f, .5f, -160.0, 225.0, N_VERTS / 4)
            sweep(coords, -0.12f, 0.55f, .45f, -55.0, 225.0, N_VERTS / 4)
            sweep(coords, 0.08f, 0.35f, .45f, 170.0, -225.0, N_VERTS / 4)
            sweep(coords, 0.08f, -0.5f, .5f, 65.0, -225.0, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("3", coords, 1.55f)
        }

        private fun make4(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -.7f, .65f, -.7f, N_VERTS / 6)
            line(coords, .45f, -.5f, -.6f, -.5f, N_VERTS / 6)
            line(coords, -.6f, -.35f, .1f, 1f, N_VERTS / 6)
            line(coords, .3f, 1f, .3f, -.8f, N_VERTS / 6)
            line(coords, .5f, -1f, .5f, .8f, N_VERTS / 6)
            line(coords, .3f, .8f, -.4f, -.55f, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("4", coords, 1.65f)
        }

        private fun make5(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, -0.12f, -0.25f, .55f, -160.0, 300.0, N_VERTS / 4)
            line(coords, -.3f, 1f, .4f, 1f, N_VERTS / 4)
            line(coords, .6f, .8f, -.1f, .8f, N_VERTS / 4)
            sweep(coords, 0.08f, -0.45f, .55f, 140.0, -300.0, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("5", coords, 1.65f)
        }

        private fun make6(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 0.1f, -0.45f, .55f, -210.0, 310.0, N_VERTS / 2 - 5)
            sweep(coords, -0.1f, -0.25f, .55f, 100.0, -310.0, N_VERTS / 2 - 5)
            line(coords, .15f, 1f, .35f, .8f, 9)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("6", coords, 1.75f)
        }

        private fun make7(): AnimateableChar {
            val coords = FloatList()
            line(coords, .0f, -1f, .6f, .65f, N_VERTS / 4)
            line(coords, .6f, .8f, -.4f, .8f, N_VERTS / 4)
            line(coords, -.6f, 1f, .4f, 1f, N_VERTS / 4)
            line(coords, .4f, .85f, -.2f, -.8f, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("7", coords, 1.65f)
        }

        private fun make8(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 0f, -0.4f, .58f, 125.0, 290.0, N_VERTS / 2)
            sweep(coords, 0f, 0.48f, .52f, -45.0, 270.0, N_VERTS / 2 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("8", coords, 1.63f)
        }

        private fun make9(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.15f, -1f, -.35f, -.8f, 9)
            sweep(coords, -0.1f, 0.45f, .55f, -30.0, 310.0, N_VERTS / 2 - 5)
            sweep(coords, 0.1f, 0.25f, .55f, -80.0, -310.0, N_VERTS / 2 - 5)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("9", coords, 1.75f)
        }

        private fun makeComma(): AnimateableChar {
            val coords = FloatList()
            line(coords, -1f, -.9f, -1f, -.95f, 2)
            line(coords, -1f, -.95f, -.85f, -1.1f, 2)
            return AnimateableChar(",", coords, 0f)
        }

        private fun makePlus(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.1f, -.4f, .1f, -.6f, N_VERTS / 5)
            line(coords, .1f, -.1f, .6f, -.1f, N_VERTS / 5)
            line(coords, .4f, .1f, -.6f, .1f, N_VERTS / 5)
            line(coords, -.4f, -.1f, .1f, -.1f, N_VERTS / 5)
            line(coords, .1f, .4f, -.1f, .6f, N_VERTS / 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("+", coords, 1.5f)
        }

        private fun makeMinus(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -.1f, .6f, -.1f, N_VERTS / 2)
            line(coords, .4f, .1f, -.6f, .1f, N_VERTS / 2 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("-", coords, 1.5f)
        }

        private fun makeTimes(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -.6f, 0f, -.2f, N_VERTS / 6)
            line(coords, .4f, -.6f, .6f, -.4f, N_VERTS / 6)
            line(coords, .2f, 0f, .6f, .4f, N_VERTS / 6)
            line(coords, .4f, .6f, 0f, .2f, N_VERTS / 6)
            line(coords, -.4f, .6f, -.6f, .4f, N_VERTS / 6)
            line(coords, -.2f, 0f, -.6f, -.4f, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            scale(coords, .8f)
            return AnimateableChar("*", coords, 1.5f)
        }

        private fun makeSlash(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -1f, .4f, 1f, N_VERTS)
            return AnimateableChar("/", coords, 1f)
        }

        private fun makePow(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.25f, .3f, .15f, .8f, N_VERTS / 4)
            line(coords, .15f, .8f, .55f, .3f, N_VERTS / 4)
            line(coords, .3f, .5f, -.1f, 1f, N_VERTS / 4)
            line(coords, -.1f, 1f, -.5f, .5f, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("^", coords, 1.3f)
        }

        private fun makeParenOpen(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 1f, .1f, 1.28f, -135.0, -90.0, N_VERTS / 2)
            sweep(coords, 1.1f, -.1f, 1.28f, 135.0, 90.0, N_VERTS / 2 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar("(", coords, .85f)
        }

        private fun makeParenClose(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, -1f, -.1f, 1.28f, -45.0, 90.0, N_VERTS / 2)
            sweep(coords, -1.1f, .1f, 1.28f, 45.0, -90.0, N_VERTS / 2 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            return AnimateableChar(")", coords, .85f)
        }

        private fun makeS(small: Boolean): AnimateableChar {
            val coords = FloatList()
            line(coords, -.55f, -.8f, -.35f, -1f, N_VERTS / 6)
            sweep(coords, .1f, -.55f, .45f, -90.0, 180.0, N_VERTS / 6)
            sweep(coords, .1f, .35f, .45f, -90.0, -180.0, N_VERTS / 6)
            line(coords, .55f, .8f, .35f, 1f, N_VERTS / 6)
            sweep(coords, -.1f, .55f, .45f, 90.0, 180.0, N_VERTS / 6)
            sweep(coords, -.1f, -.35f, .45f, 90.0, -180.0, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            var w = 1.1f
            if (small) {
                transform(coords, .7f, -.3f)
                w *= 0.7f
            }
            return AnimateableChar("s", coords, w + 0.5f)
        }

        private fun makeI(small: Boolean): AnimateableChar {
            val coords = FloatList()
            line(coords, -.2f, -1f, .2f, -1f, N_VERTS / 6)
            line(coords, .2f, -1f, .2f, 1f, N_VERTS / 3)
            line(coords, .2f, 1f, -.2f, 1f, N_VERTS / 3)
            line(coords, -.2f, 1f, -.2f, -1f, N_VERTS / 6)
            var w = .4f
            if (small) {
                transform(coords, .7f, -.3f)
                w *= 0.7f
            }
            return AnimateableChar("i", coords, w + 0.5f)
        }

        private fun make_n(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.65f, -.8f, -.45f, -1f, N_VERTS / 4)
            sweep(coords, .1f, .25f, .55f, 180.0, -180.0, N_VERTS / 4)
            line(coords, .65f, -1f, .45f, -.8f, N_VERTS / 4)
            sweep(coords, -.1f, .45f, .55f, 0.0, 180.0, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            var w = 1.3f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("n", coords, w + 0.5f)
        }

        private fun makeC(small: Boolean): AnimateableChar {
            val coords = FloatList()
            line(coords, .6f, -1f, .4f, -.8f, N_VERTS / 6)
            sweep(coords, .1f, -.1f, .7f, -90.0, -90.0, N_VERTS / 6)
            sweep(coords, .1f, .3f, .7f, -180.0, -90.0, N_VERTS / 6)
            line(coords, .4f, 1f, .6f, .8f, N_VERTS / 6)
            sweep(coords, .3f, .1f, .7f, 90.0, 90.0, N_VERTS / 6)
            sweep(coords, .3f, -.3f, .7f, 180.0, 90.0, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))
            var w = 1f
            if (small) {
                transform(coords, .7f, -.3f)
                w *= 0.7f
            }
            return AnimateableChar("c", coords, w + 0.5f)
        }

        private fun makeO(small: Boolean): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 0f, 0f, 1f, -90.0, 360.0, N_VERTS)
            var w = 2f
            if (small) {
                transform(coords, .7f, -.3f)
                w *= 0.7f
            }
            return AnimateableChar("o", coords, w + 0.25f)
        }

        private fun make_a(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, -.1f, .1f, .9f, -45.0, -225.0, N_VERTS / 4)
            line(coords, .8f, 1f, .8f, -.8f, N_VERTS / 4)
            line(coords, 1f, -1f, 1f, .8f, N_VERTS / 4)
            sweep(coords, .1f, -.1f, .9f, 90.0, 225.0, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            var w = 2f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("a", coords, w + 0.25f)
        }

        private fun make_e(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, 0f, -.05f, .75f, -48.0, -132.0, N_VERTS / 6)
            sweep(coords, -.1f, .35f, .65f, 180.0, -180.0, N_VERTS / 6)
            line(coords, .55f, .2f, -.6f, .2f, N_VERTS / 6)
            line(coords, -.4f, 0f, .75f, 0f, N_VERTS / 6)
            sweep(coords, .1f, .15f, .65f, 0.0, 180.0, N_VERTS / 6)
            sweep(coords, .2f, -.25f, .75f, -180.0, 132.0, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            var w = 1.3f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("e", coords, w + 0.25f)
        }

        private fun make_r(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -1f, -.4f, .3f, N_VERTS / 4)
            sweep(coords, .1f, .3f, .5f, -180.0, -180.0, N_VERTS / 4)
            sweep(coords, -.1f, .5f, .5f, 0.0, 180.0, N_VERTS / 4)
            line(coords, -.6f, .5f, -.6f, -.8f, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            var w = 1.3f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("r", coords, w + 0.25f)
        }

        private fun make_l(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, .2f, -.3f, .5f, -90.0, -90.0, N_VERTS / 4)
            line(coords, -.3f, -.3f, -.3f, 1f, N_VERTS / 4)
            line(coords, -.1f, .8f, -.1f, -.5f, N_VERTS / 4)
            sweep(coords, .4f, -.5f, .5f, 180.0, 90.0, N_VERTS / 4 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            val w = .7f
            return AnimateableChar("l", coords, w + 0.25f)
        }

        private fun make_g(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, -.1f, .1f, .9f, -45.0, -225.0, N_VERTS / 6)
            line(coords, .8f, 1f, .8f, -.8f, N_VERTS / 6)
            sweep(coords, -.1f, -1f, .9f, 0.0, -180.0, N_VERTS / 6)
            sweep(coords, .1f, -1.2f, .9f, 180.0, 180.0, N_VERTS / 6)
            line(coords, 1f, -1f, 1f, .8f, N_VERTS / 6)
            sweep(coords, .1f, -.1f, .9f, 90.0, 225.0, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            var w = 2f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("g", coords, w + 0.25f)
        }

        private fun make_t(): AnimateableChar {
            val coords = FloatList()
            sweep(coords, .6f, -.5f, .5f, 180.0, 90.0, N_VERTS / 6)
            sweep(coords, .4f, -.3f, .5f, -90.0, -90.0, N_VERTS / 6)
            line(coords, -.1f, .4f, -.6f, .4f, N_VERTS / 6)
            line(coords, -.4f, .2f, .6f, .2f, N_VERTS / 6)
            line(coords, .4f, .4f, -.1f, .4f, N_VERTS / 6)
            line(coords, -.1f, 1f, .1f, .8f, N_VERTS - N_VERTS / 6 * 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            val w = 1.2f
            return AnimateableChar("t", coords, w + 0.25f)
        }

        private fun makePi(): AnimateableChar {
            val coords = FloatList()
            line(coords, -.4f, -1f, -.6f, -.8f, N_VERTS / 8)
            line(coords, -.6f, 1f, .4f, 1f, N_VERTS / 8)
            line(coords, .4f, -.8f, .6f, -1f, N_VERTS / 8)
            line(coords, .6f, .8f, 1f, .8f, N_VERTS / 8)
            line(coords, .8f, 1f, .4f, 1f, N_VERTS / 8)
            line(coords, .6f, .8f, -.4f, .8f, N_VERTS / 8)
            line(coords, -.6f, 1f, -1f, 1f, N_VERTS / 8)
            line(coords, -.8f, .8f, -.4f, .8f, N_VERTS - N_VERTS / 8 * 7 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            var w = 2f
            transform(coords, .7f, -.3f)
            w *= 0.7f
            return AnimateableChar("pi", coords, w + 0.25f)
        }

        private fun makeSqrt(): AnimateableChar {
            val coords = FloatList()
            line(coords, 0f, -1f, .4f, .8f, N_VERTS / 5)
            line(coords, .8f, .8f, .6f, 1f, N_VERTS / 5)
            line(coords, .2f, 1f, -.2f, -.8f, N_VERTS / 5)
            line(coords, -.5f, 0f, -.8f, 0f, N_VERTS / 5)
            line(coords, -.6f, -.2f, -.3f, -.2f, N_VERTS / 5 - 1)
            coords.add(coords.get(0))
            coords.add(coords.get(1))
            coords.add(coords.get(2))

            val w = 1.6f
            return AnimateableChar("sqrt", coords, w + 0.25f)
        }

        private fun scale(list: FloatList, fac: Float) {
            transform(list, fac, 0f)
        }

        private fun transform(list: FloatList, fac: Float, tY: Float) {
            var i = 0
            while (i < list.size()) {
                list.set(i, list.get(i) * fac)
                list.set(i + 1, list.get(i + 1) * fac + tY)
                list.set(i + 2, list.get(i + 2) * fac)
                i += 3
            }
        }

        private fun line(list: FloatList, x0: Float, y0: Float, x1: Float, y1: Float, n: Int) {
            for (i in 0..n - 1) {
                list.add(x0 + (x1 - x0) * i / (n - 1))
                list.add(y0 + (y1 - y0) * i / (n - 1))
                list.add(0f)
            }
        }

        private fun sweep(list: FloatList, cx: Float, cy: Float, r: Float, start: Double, sweep: Double, n: Int) {
            var startRad = start
            var sweepRad = sweep
            startRad = Math.toRadians(startRad)
            sweepRad = Math.toRadians(sweepRad)
            for (i in 0..n - 1) {
                val x = Math.cos(startRad + sweepRad * i / (n - 1)).toFloat() * r + cx
                val y = Math.sin(startRad + sweepRad * i / (n - 1)).toFloat() * r + cy
                list.add(x)
                list.add(y)
                list.add(0f)
            }
        }
    }
}
