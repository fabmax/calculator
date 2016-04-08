package de.fabmax.calc

import android.content.res.Configuration
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import de.fabmax.calc.layout.phoneLayout
import de.fabmax.lightgl.*
import de.fabmax.lightgl.scene.Node
import de.fabmax.lightgl.scene.TransformGroup
import de.fabmax.lightgl.util.Color
import de.fabmax.lightgl.util.GlMath
import de.fabmax.lightgl.util.Painter

class MainActivity : LightGlActivity() {

    private val mRotationSens = AccelSensor()
    private var mParallax: ParallaxHelper? = null
    private var mContentGrp = TransformGroup()
    private var mContent: Content? = null

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private val mScreenBounds = BoundingBox(0f, 0f, 0f)

    private var mTouchX = -1f
    private var mTouchY = -1f
    private var mTouchAction = -1

    private val mCamPos = Vec3fAnimation()
    private val mCamLookAt = Vec3fAnimation()
    private var m3rdPerson = false
    private val mCamTmp = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mParallax = ParallaxHelper(this)
        mParallax?.setIntensity(0.5f, 1f)
        mParallax?.invertAxis(true, false)

        setNumSamples(4)
        createEngine(false)
        setLogFramesPerSecond(true)

        val cam = mEngine.camera as PerspectiveCamera
        cam.fovy = 40.0f
        mRotationSens.setCamera(cam)

        mGlView.setOnTouchListener({ view, event ->
            // store touch event in order to process it within the OpenGL thread
            mTouchX = event.x
            mTouchY = event.y
            mTouchAction = event.actionMasked
            true
        })
    }

    override fun onResume() {
        super.onResume()
        mParallax?.onResume()
        mRotationSens.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        mParallax?.onPause()
        mRotationSens.onPause(this)
    }

    override fun onViewportChange(width: Int, height: Int) {
        super.onViewportChange(width, height);
        Log.d("Activity", "onViewportChange: $width x $height")

        val cam = mEngine.camera as PerspectiveCamera
        val fovy = cam.fovy
        val d = ((height * 0.5) / Math.tan(Math.toRadians(fovy / 2.0))).toFloat();

        cam.setPosition(0.0f, 0.0f, -d)
        mCamPos.start(d*0.45f, -d*0.6f, -d*0.75f, 0.0f, 0.0f, -d).overTime(0f, true)
        mCamLookAt.start(width/20f, -height/20f, 0f, 0f, 0f, 0f).overTime(0f, true)

        cam.setClipRange(100.0f, d * 2.0f - 200.0f)
        cam.setUpDirection(0.0f, -1.0f, 0.0f)

        setContentSize(width, height)
    }

    override fun onLoadScene(glContext: LightGlContext) {
        glContext.state.setBackgroundColor(1.0f, 1.0f, 1.0f)
        glContext.engine.scene = mContentGrp
        mContentGrp.resetTransform()
        mContentGrp.scale(-1f, -1f, -1f)

        // add a directional light
        val light = Light.createDirectionalLight(-.4f, .5f, -1f, 0.7f, 0.7f, 0.7f)
        glContext.engine.addLight(light)

        // enable shadow rendering
        val sceneBounds = BoundingBox(-1000f, 1000f, -1000f, 1000f, -1000f, 1000f)
        val shadow = ShadowRenderPass()
        shadow.setSceneBounds(sceneBounds)
        shadow.shadowMapSize = 2048
        glContext.engine.preRenderPass = shadow

        mContent = Content(glContext, phoneLayout(this));
        mContentGrp.addChild(mContent)

        setContentSize(mScreenWidth, mScreenHeight)
    }

    override fun onRenderFrame(glContext: LightGlContext?) {
        super.onRenderFrame(glContext)

        mContent?.layout?.mixConfigs(mRotationSens.normalizedHV)
        mParallax?.rotation = mRotationSens.rotation

        val pos = mCamPos.animate()
        val lookAt = mCamLookAt.animate()
        mCamTmp[0] = pos.x
        mCamTmp[1] = pos.y
        mCamTmp[2] = pos.z

        val light = mEngine.lights[0]
        light.position[0] = -.4f;
        light.position[1] = .5f;
        light.position[2] = -1f

        if (m3rdPerson) {
            // parallax is only active in 3rd person view
            mParallax?.transformCameraVector(mCamTmp)
            mParallax?.transformLightVector(light.position)
        }

        mEngine.camera.setPosition(mCamTmp[0], mCamTmp[1], mCamTmp[2])
        mEngine.camera.setLookAt(lookAt.x, lookAt.y, lookAt.z)
    }

    fun toggleCamera() {
        mCamPos.reverse().overTime(0.25f, true)
        mCamLookAt.reverse().overTime(0.25f, true)
        m3rdPerson = !m3rdPerson
    }

    private fun setContentSize(width: Int, height: Int) {
        mScreenWidth = width
        mScreenHeight = height

        mScreenBounds.reset(0f, 0f, 0f)
        mScreenBounds.maxX = width.toFloat()
        mScreenBounds.maxY = height.toFloat()
        mContent?.layout?.doLayout(Orientation.PORTRAIT, mScreenBounds, this)

        mScreenBounds.reset(0f, 0f, 0f)
        mScreenBounds.maxX = height.toFloat()
        mScreenBounds.maxY = width.toFloat()
        mContent?.layout?.doLayout(Orientation.LANDSCAPE, mScreenBounds, this)

        mContent?.layout?.mixConfigs(1f)
    }

    private inner class Content(glContext: LightGlContext, layout: Layout) : Node() {
        val painter = Painter(glContext)
        val pickRay = Ray()
        var layout: Layout = layout

        override fun render(context: LightGlContext) {
            if (mTouchAction != -1) {
                context.engine.camera.getPickRay(context.state.viewport, mTouchX, mTouchY, pickRay)
                GlMath.multiplyMV(mContentGrp.inverseTransformation, 0, pickRay.origin, 0);
                GlMath.multiplyMV(mContentGrp.inverseTransformation, 0, pickRay.direction, 0);
                layout.processTouch(pickRay, mTouchAction)
                mTouchAction = -1
            }

            painter.pushTransform()
            painter.translate(layout.x, layout.y, layout.z)
            layout.paint(painter)
            painter.popTransform()
        }

        override fun delete(context: LightGlContext?) {
            // nothing to do
        }
    }
}
