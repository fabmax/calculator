package de.fabmax.calc.animFont

import de.fabmax.lightgl.util.FloatList

class MutableChar(nVerts: Int) : AnimateableChar("mut", nVerts) {

    constructor() : this(AnimateableChar.N_VERTS)

    init {
        isStatic = false
    }

    fun setPath(other: AnimateableChar) {
        System.arraycopy(other.path, 0, path, 0, path.size)
        System.arraycopy(other.stepLens, 0, stepLens, 0, stepLens.size)
        pathLength = other.pathLength
    }

    fun setPath(path: FloatList) {
        path.copyToArray(this.path)
        updatePathLength()
    }

    fun setCharWidth(adv: Float) {
        charAdvance = adv
    }
}
