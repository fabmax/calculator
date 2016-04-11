package de.fabmax.calc

import android.os.Bundle
import de.fabmax.calc.layout.phoneLayout
import de.fabmax.calc.ui.Button
import de.fabmax.calc.ui.Layout
import de.fabmax.calc.ui.Orientation
import de.fabmax.lightgl.*
import de.fabmax.lightgl.scene.Node
import de.fabmax.lightgl.scene.TransformGroup
import de.fabmax.lightgl.util.GlMath
import de.fabmax.lightgl.util.Painter

/**
 * The one and only Activity in this project. Extends LightGlActivity which does most of the
 * OpenGL related heavy lifting. The only content is a GLSurfaceView.
 */
class MainActivity : LightGlActivity() {

    private val mRotationSens = RotationSensor()
    private var mParallax: ParallaxHelper? = null
    private var mContentGrp = TransformGroup()
    private var mContent: Content? = null

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private val mScreenBounds = BoundingBox(0f, 0f, 0f)

    private val mCamPos = Vec3fAnimation()
    private val mCamLookAt = Vec3fAnimation()
    private val mCamTmp = FloatArray(3)
    private var mPerspectiveMode = false

    private val mTouch = object {
        var posX = -1f
        var posY = -1f
        var action = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ParallaxHelper makes the camera wobble a bit when in perspective mode
        mParallax = ParallaxHelper(this)
        mParallax?.setIntensity(0.5f, 1f)
        mParallax?.invertAxis(true, false)

        // Create the engine
        setNumSamples(4)
        createEngine(false)
        //setLogFramesPerSecond(true)

        // Create a perspective camera with 40° field of view
        val cam = PerspectiveCamera()
        cam.fovy = 40.0f
        mEngine.camera = cam

        // Install a OnTouchListener which saves the touch events for later evaluation from
        // within the render loop
        mGlView.setOnTouchListener({ view, event ->
            // store touch event in order to process it within the OpenGL thread
            mTouch.posX = event.x
            mTouch.posY = event.y
            mTouch.action = event.actionMasked
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

    /**
     * Called when the OpenGL viewport size has changed.
     */
    override fun onViewportChange(width: Int, height: Int) {
        super.onViewportChange(width, height);

        // Adjust camera position so that at z = 0 one pixel corresponds to exactly one unit in
        // GL coordinates
        val cam = mEngine.camera as PerspectiveCamera
        val fovy = cam.fovy
        val d = ((height * 0.5) / Math.tan(Math.toRadians(fovy / 2.0))).toFloat();
        cam.setPosition(0.0f, 0.0f, -d)
        cam.setClipRange(100.0f, d * 2.0f - 200.0f)
        cam.setUpDirection(0.0f, -1.0f, 0.0f)

        // setup camera animation for toggling between standard mode and perspective mode
        // animations are not started yet
        mCamPos.set(0.0f, 0.0f, -d, d*0.45f, -d*0.6f, -d*0.75f)
        mCamPos.whenDone = { -> mCamPos.reverse(); mPerspectiveMode = !mPerspectiveMode }
        mCamLookAt.set(0f, 0f, 0f, width/20f, -height/20f, 0f)
        mCamLookAt.whenDone = { -> mCamLookAt.reverse() }

        // do layout computations for new viewport size
        setContentSize(width, height)
    }

    /**
     * Called when the OpenGL context is created, does the initial scene setup.
     */
    override fun onLoadScene(glContext: LightGlContext) {
        // basic engine setup
        glContext.state.setBackgroundColor(1.0f, 1.0f, 1.0f)
        glContext.state.setDepthTesting(false)
        glContext.engine.scene = mContentGrp

        // create a directional light, actual direction is set in onRenderFrame()
        // light color is currently ignored
        val light = Light.createDirectionalLight(1f, 1f, 1f, 1f, 1f, 1f)
        glContext.engine.addLight(light)

        // enable dynamic shadow rendering
        val sceneBounds = BoundingBox(-1000f, 1000f, -1000f, 1000f, -1000f, 1000f)
        val shadow = ShadowRenderPass()
        shadow.setSceneBounds(sceneBounds)
        shadow.shadowMapSize = 2048
        glContext.engine.preRenderPass = shadow

        // Load the layout and set it as content
        mContent = Content(glContext, phoneLayout(this));
        mContentGrp.addChild(mContent)
        mContentGrp.resetTransform()
        // invert axis to get intuitive coordinates
        mContentGrp.scale(-1f, -1f, -1f)

        // do layout computations for current viewport size
        setContentSize(mScreenWidth, mScreenHeight)
    }

    /**
     * Called before every rendered frame.
     */
    override fun onRenderFrame(glContext: LightGlContext?) {
        super.onRenderFrame(glContext)

        // interpolate layout between portrait and landscape for current orientation
        mContent?.layout?.mixConfigs(mRotationSens.normalizedHV)
        // ParallaxHelper needs to know the orientation as well...
        mParallax?.rotation = mRotationSens.rotation

        // animate camera between normal mode and perspective mode
        // position remains static as long as the animation is not started
        val pos = mCamPos.animate()
        val lookAt = mCamLookAt.animate()
        mCamTmp[0] = pos.x
        mCamTmp[1] = pos.y
        mCamTmp[2] = pos.z

        // reset light direction
        val light = mEngine.lights[0]
        light.position[0] = -.4f;
        light.position[1] = .5f;
        light.position[2] = -1f

        if (mPerspectiveMode) {
            // if perspective mode is active, camera position and light direction are modified
            // based on the device gyroscope
            mParallax?.transformCameraVector(mCamTmp)
            mParallax?.transformLightVector(light.position)
        }

        // update camera position and up direction for this frame
        val upDir = mRotationSens.upDirection
        mEngine.camera.setUpDirection(upDir.x, upDir.y, upDir.z)
        mEngine.camera.setPosition(mCamTmp[0], mCamTmp[1], mCamTmp[2])
        mEngine.camera.setLookAt(lookAt.x, lookAt.y, lookAt.z)
    }

    /**
     * Toggles the camera mode between standard and perspective mode. The animations are
     * automatically setup for the reverse direction when finished
     */
    fun toggleCamera() {
        mCamPos.start(0.25f, true)
        mCamLookAt.start(0.25f, true)
    }

    /**
     * Called when the user hits the 'inv' button. Flips the 'sin', 'cos' and 'tan' function
     * buttons to show their inverse functions and vice versa.
     */
    fun flipFunctions() {
        val layout = mContent?.layout
        if (layout != null) {
            val sin = layout.findById("sin") as Button
            val cos = layout.findById("cos") as Button
            val tan = layout.findById("tan") as Button

            if (sin.text == "sin") {
                sin.flipText("asin")
                cos.flipText("acos")
                tan.flipText("atan")
            } else {
                sin.flipText("sin")
                cos.flipText("cos")
                tan.flipText("tan")
            }
        }
    }

    /**
     * Updates screen bounds and layout configurations for the given viewport size.
     */
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

    /**
     * Content is the root node for all OpenGL UI elements.
     */
    private inner class Content(glContext: LightGlContext, layout: Layout) : Node() {
        val painter = Painter(glContext)
        val pickRay = Ray()
        var layout: Layout = layout

        /**
         * Renders the layout.
         */
        override fun render(context: LightGlContext) {
            if (mTouch.action != -1) {
                // compute a pick ray for the current touch input and process touch action
                context.engine.camera
                        .getPickRay(context.state.viewport, mTouch.posX, mTouch.posY, pickRay)
                GlMath.multiplyMV(mContentGrp.inverseTransformation, 0, pickRay.origin, 0);
                GlMath.multiplyMV(mContentGrp.inverseTransformation, 0, pickRay.direction, 0);
                layout.processTouch(pickRay, mTouch.action)
                mTouch.action = -1
            }

            // do the drawing!
            painter.pushTransform()
            painter.translate(layout.x, layout.y, layout.z)
            layout.paint(painter)
            painter.popTransform()
        }

        override fun delete(context: LightGlContext?) {
            // Content is never deleted
        }
    }
}
