package de.fabmax.calc

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

import java.util.Locale

import de.fabmax.lightgl.Camera

/**
 * Acceleration sensor handler for detecting screen rotations
 */
class AccelSensor : SensorEventListener {

    private var mCam: Camera? = null
    private val mFilter = Filter()
    private var mRotation = 0f

    fun setCamera(cam: Camera) {
        mCam = cam
    }

    fun onResume(context: Context) {
        val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorMgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun onPause(context: Context) {
        val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorMgr.unregisterListener(this)
    }

    val normalizedHV: Float
        get() {
            val r = Math.abs(mRotation)
            if (r < PI_2) {
                return 1 - r / PI_2
            } else {
                return (r - PI_2) / PI_2
            }
        }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val m = Math.sqrt((x * x + y * y).toDouble()).toFloat()

        var a = (Math.atan2(x.toDouble(), y.toDouble())).toFloat()
        if (m > MAG_THRESH) {
            // if magnitude is large enough, determine screen orientation from acceleration vector
            if (Math.abs(a) < ROT_THRESH) {
                a = 0f
            } else if (Math.abs(a - PI_2) < ROT_THRESH) {
                a = PI_2
            } else if (Math.abs(a + PI_2) < ROT_THRESH) {
                a = -PI_2
            } else if (Math.abs(a - PI) < ROT_THRESH) {
                a = PI
            } else if (Math.abs(a + PI) < ROT_THRESH) {
                a = -PI
            }
        } else {
            // else lock screen orientation to nearest 90° step
            if (Math.abs(mRotation) < PI_4) {
                a = 0f
            } else if (Math.abs(mRotation - PI_2) < PI_4) {
                a = PI_2
            } else if (Math.abs(mRotation + PI_2) < PI_4) {
                a = -PI_2
            } else if (Math.abs(mRotation - PI) < PI_4) {
                a = PI
            } else if (Math.abs(mRotation + PI) < PI_4) {
                a = -PI
            }
        }
        mRotation = mFilter.update(a)
        //mCam?.setUpDirection((-Math.cos(mRotation.toDouble())).toFloat(), Math.sin(mRotation.toDouble()).toFloat(), 0f)
        mCam?.setUpDirection((Math.sin(mRotation.toDouble())).toFloat(), Math.cos(mRotation.toDouble()).toFloat(), 0f)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // ignored
    }

    private inner class Filter {
        internal var mBuf = FloatArray(8)
        internal var mIdx = 0

        internal fun update(f: Float): Float {
            mBuf[mIdx++] = f
            if (mIdx >= mBuf.size) {
                mIdx = 0
            }

            val first = mBuf[0]
            var out = first
            for (i in 1..mBuf.size - 1) {
                var x = mBuf[i]
                if (Math.abs(x - first) > PI) {
                    if (x < 0) {
                        x += PI * 2
                    } else {
                        x -= PI * 2
                    }
                }
                out += x
            }
            return out / mBuf.size
        }
    }

    companion object {
        private val PI = Math.PI.toFloat()
        private val PI_2 = PI / 2
        private val PI_4 = PI / 4
        private val ROT_THRESH = Math.toRadians(20.0).toFloat()
        private val MAG_THRESH = 4.0f
    }
}
