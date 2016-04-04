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
import de.fabmax.lightgl.util.GlMath
import de.fabmax.lightgl.util.Painter

class MainActivity : LightGlActivity() {

    private val mRotationSens = AccelSensor()
    private var contentGrp = TransformGroup()
    private var mContent: Content? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private val screenBounds = BoundingBox(0f, 0f, 0f)

    private var touchX = -1f
    private var touchY = -1f
    private var touchAction = -1

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createEngine(false)
        //setLogFramesPerSecond(true)

        val cam = mEngine.camera as PerspectiveCamera
        cam.fovy = 40.0f
        mRotationSens.setCamera(cam)

        mGlView.setOnTouchListener({ view, event ->
            // store touch event in order to process it within the OpenGL thread
            touchX = event.x
            touchY = event.y
            touchAction = event.actionMasked
            true
        })
    }

    override fun onResume() {
        super.onResume()
        mRotationSens.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        mRotationSens.onPause(this)
    }

    override fun onViewportChange(width: Int, height: Int) {
        super.onViewportChange(width, height);
        Log.d("Activity", "onViewportChange: $width x $height")

        val cam = mEngine.camera as PerspectiveCamera
        val fovy = cam.fovy
        val d = ((height * 0.5) / Math.tan(Math.toRadians(fovy / 2.0))).toFloat();

        cam.setPosition(0.0f, 0.0f, -d);
        cam.setClipRange(100.0f, d * 2.0f - 200.0f);
        cam.setUpDirection(0.0f, -1.0f, 0.0f);

        setContentSize(width, height)
    }

    override fun onLoadScene(glContext: LightGlContext) {
        glContext.state.setBackgroundColor(1.0f, 1.0f, 1.0f)
        contentGrp.resetTransform()
        contentGrp.scale(-1f, -1f, -1f)
        mEngine.scene = contentGrp

        mContent = Content(glContext, phoneLayout(applicationContext));
        contentGrp.addChild(mContent)

        setContentSize(screenWidth, screenHeight)
    }

    private fun setContentSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        screenBounds.reset(0f, 0f, 0f)
        screenBounds.maxX = width.toFloat()
        screenBounds.maxY = height.toFloat()
        mContent?.layout?.doLayout(Orientation.PORTRAIT, screenBounds, this)

        screenBounds.reset(0f, 0f, 0f)
        screenBounds.maxX = height.toFloat()
        screenBounds.maxY = width.toFloat()
        mContent?.layout?.doLayout(Orientation.LANDSCAPE, screenBounds, this)

        mContent?.layout?.mixConfigs(1f)
    }

    private inner class Content(glContext: LightGlContext, layout: Layout) : Node() {
        val painter = Painter(glContext)
        val pickRay = Ray()
        var layout: Layout = layout

        override fun render(context: LightGlContext) {
            layout.mixConfigs(mRotationSens.normalizedHV)

            if (touchAction != -1) {
                context.engine.camera.getPickRay(context.state.viewport, touchX, touchY, pickRay)
                GlMath.multiplyMV(contentGrp.inverseTransformation, 0, pickRay.origin, 0);
                GlMath.multiplyMV(contentGrp.inverseTransformation, 0, pickRay.direction, 0);
                layout.processTouch(pickRay, touchAction)
                touchAction = -1
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
