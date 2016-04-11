package de.fabmax.calc.animFont

import de.fabmax.lightgl.util.FloatList

class MutableChar(nVerts: Int) : AnimateableChar("mut", nVerts) {

    constructor() : this(AnimateableChar.N_VERTS)

    init {
        mIsStatic = false
    }

    fun setPath(other: AnimateableChar) {
        System.arraycopy(other.mPath, 0, mPath, 0, mPath.size)
        System.arraycopy(other.mStepLens, 0, mStepLens, 0, mStepLens.size)
        mPathLength = other.mPathLength
    }

    fun setPath(path: FloatList) {
        path.copyToArray(this.mPath)
        updatePathLength()
    }

    fun setCharWidth(adv: Float) {
        charAdvance = adv
    }
}
