package de.fabmax.calc

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Acceleration sensor handler for detecting screen rotations
 */
class RotationSensor : SensorEventListener {

    private val mFilter = Filter()
    private var mSnappedIn = true

    var rotation = 0f
    val upDirection = Vec3f(0f, 1f, 0f)

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

    /**
     * Normalized orientation: 1 = portrait, 0 = landscape
     */
    val normalizedHV: Float
        get() {
            val r = Math.abs(rotation)
            if (r < PI_2) {
                return 1 - r / PI_2
            } else {
                return (r - PI_2) / PI_2
            }
        }

    /**
     * Here happens most of the magic: Camera up direction is computed based on the phone's
     * acceleration sensor. Also up direction snaps in when the orientation is close to a
     * 90° angle or the phone is to horizontal.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val m = Math.sqrt((x * x + y * y).toDouble()).toFloat()

        val thresh = when(mSnappedIn) {
            true -> ROT_THRESH
            else -> ROT_THRESH / 4
        }
        val zThresh = when(mSnappedIn) {
            true -> MAG_THRESH_Z_SNAPPED
            else -> MAG_THRESH_Z
        }

        var a = (Math.atan2(x.toDouble(), y.toDouble())).toFloat()
        if (m > MAG_THRESH && z < zThresh) {
            // if magnitude is large enough, determine screen orientation from acceleration vector
            if (Math.abs(a) < thresh) {
                a = 0f
            } else if (Math.abs(a - PI_2) < thresh) {
                a = PI_2
            } else if (Math.abs(a + PI_2) < thresh) {
                a = -PI_2
            } else if (Math.abs(a - PI) < thresh) {
                a = PI
            } else if (Math.abs(a + PI) < thresh) {
                a = -PI
            }
        } else {
            // else lock screen orientation to nearest 90° step
            if (Math.abs(rotation) < PI_4) {
                a = 0f
            } else if (Math.abs(rotation - PI_2) < PI_4) {
                a = PI_2
            } else if (Math.abs(rotation + PI_2) < PI_4) {
                a = -PI_2
            } else if (Math.abs(rotation - PI) < PI_4) {
                a = PI
            } else if (Math.abs(rotation + PI) < PI_4) {
                a = -PI
            }
        }
        mSnappedIn = Math.abs((a % PI_2).toDouble()) < 0.0001

        rotation = mFilter.update(a)
        upDirection.x = Math.sin(rotation.toDouble()).toFloat()
        upDirection.y = Math.cos(rotation.toDouble()).toFloat()
        upDirection.z = 0f
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // ignored
    }

    private inner class Filter {
        var mBuf = FloatArray(12)
        var mIdx = 0

        fun update(f: Float): Float {
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
        private val ROT_THRESH = Math.toRadians(30.0).toFloat()
        private val MAG_THRESH = 5.0f
        private val MAG_THRESH_Z = 8.0f
        private val MAG_THRESH_Z_SNAPPED = 5.0f
    }
}
