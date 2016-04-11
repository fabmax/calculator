package de.fabmax.calc

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager

import de.fabmax.lightgl.util.GlMath

/**
 * ParallaxHelper uses the gyro to compute an orientation matrix which can be used to update
 * a camera position and light direction. If no gyro is available on the device the acceleration
 * sensor is used as fallback.
 */
class ParallaxHelper(context: Context) : SensorEventListener {

    private val mDisplay: Display
    private val mSensorManager: SensorManager
    private val mGyro: Sensor?
    private val mAcceleration: Sensor?
    private var mRegistered = false
    private var mTimestamp = 0L

    // delta rotation measured by sensor
    private val mSensorRotation = FloatArray(3)
    private val mAccelSensorRotation = FloatArray(12)
    // rotation quaternion computed on sensor update
    private val mDeltaRotationVector = FloatArray(4)
    // 3x3 rotation matrix computed from quaternion on sensor update
    private val mDeltaRotationMatrix = FloatArray(9)
    // OpenGL transform matrices for camera, light and temp buffers
    private val mRotationMatrix = FloatArray(64)
    private val mTestVec = FloatArray(3)

    // controls how strong the gyro affects the rotation matrix
    private var mIntensity = 1.0f
    private var mIntensityLight = 1.0f

    private var mInvertX = false;
    private var mInvertY = false;

    var rotation = 0f

    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (mGyro == null) {
            // use acceleration sensor as fallback
            mAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            mAcceleration = null
        }

        // we need the window manager to get the screen rotation
        mDisplay = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    fun onResume() {
        if (!mRegistered) {
            mTimestamp = 0
            Matrix.setIdentityM(mRotationMatrix, ROT_CAMERA_OFF)
            Matrix.setIdentityM(mRotationMatrix, ROT_LIGHT_OFF)

            mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF] = 0f
            mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 1] = 0f
            mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 2] = 0f
            mAccelSensorRotation[ACCEL_LAST_VEC_OFF] = 0f
            mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 1] = 0f
            mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 2] = 0f

            if (mGyro != null) {
                Log.d(TAG, "register gyro listener")
                mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME)
                mRegistered = true

            } else if (mAcceleration != null) {
                Log.d(TAG, "register acceleration listener")
                mSensorManager.registerListener(this, mAcceleration, SensorManager.SENSOR_DELAY_GAME)
                mRegistered = true

            }
        }
    }

    fun onPause() {
        if (mRegistered) {
            Log.d(TAG, "unregister gyro listener")
            mSensorManager.unregisterListener(this)
            mRegistered = false
        }
    }

    fun setIntensity(intensityObj: Float, intensityLight: Float) {
        mIntensity = intensityObj
        mIntensityLight = intensityLight
    }

    fun invertAxis(invertX: Boolean, invertY: Boolean) {
        mInvertX = invertX
        mInvertY = invertY
    }

    fun clearLightRotationMatrix() {
        Matrix.setIdentityM(mRotationMatrix, ROT_LIGHT_OFF)
    }

    fun transformLightVector(vec3: FloatArray) {
        synchronized (mRotationMatrix) {
            transformVec3(vec3, mRotationMatrix, ROT_LIGHT_OFF)
        }
    }

    fun transformCameraVector(vec3: FloatArray) {
        synchronized (mRotationMatrix) {
            transformVec3(vec3, mRotationMatrix, ROT_CAMERA_OFF)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        // compute time since last method call in seconds
        val dT = (event.timestamp - mTimestamp) * 1e-9f

        if (mTimestamp != 0L && dT != 0f) {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                mSensorRotation[0] = event.values[0]
                mSensorRotation[1] = event.values[1]
                mSensorRotation[2] = event.values[2]

                if (mInvertX) {
                    mSensorRotation[0] = -mSensorRotation[0]
                }
                if (mInvertY) {
                    mSensorRotation[1] = -mSensorRotation[1]
                }

                // convert rotation axis from device coordinates to screen coordinates
                considerDisplayRotation(mSensorRotation, 0)


            } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // make some magic
                computeDeltaAccelRotation(event.values)
            }

            // compute rotation matrices for camera and light
            integrateSensorRotation(dT)
        }
        mTimestamp = event.timestamp
    }

    private fun computeDeltaAccelRotation(accelValues: FloatArray) {
        mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF] = accelValues[0]
        mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 1] = accelValues[1]
        mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 2] = accelValues[2]

        // convert rotation axis from device coordinates to screen coordinates
        considerDisplayRotation(mAccelSensorRotation, ACCEL_CURRENT_VEC_OFF)

        val x = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF]
        val y = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 1]
        val z = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 2]
        val lx = mAccelSensorRotation[ACCEL_LAST_VEC_OFF]
        val ly = mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 1]
        val lz = mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 2]
        mAccelSensorRotation[ACCEL_LAST_VEC_OFF] = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF]
        mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 1] = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 1]
        mAccelSensorRotation[ACCEL_LAST_VEC_OFF + 2] = mAccelSensorRotation[ACCEL_CURRENT_VEC_OFF + 2]

        // determine delta rotation around x-axis
        var rot = 0f
        var m = y * y + z * z
        var ml = ly * ly + lz * lz
        if (m > EPSILON_ACCEL && ml > EPSILON_ACCEL) {
            m = Math.sqrt(m.toDouble()).toFloat()
            ml = Math.sqrt(ml.toDouble()).toFloat()
            var cos = y / m * (lz / ml) + z / m * (-ly / ml)
            if (cos > 1) {
                cos = 1f
            } else if (cos < -1) {
                cos = -1f
            }
            rot = 90.0f - Math.acos(cos.toDouble()).toFloat() / GlMath.PI * 180.0f
        }
        // integrate rotation
        mAccelSensorRotation[ACCEL_ROT_OFF] = (mAccelSensorRotation[ACCEL_ROT_OFF] + rot) * (1 - PULL_BACK * 2)
        // filter integrated rotation
        rot = mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF] * 0.85f + mAccelSensorRotation[ACCEL_ROT_OFF] * 0.15f
        // compute delta of integrated rotation
        mSensorRotation[0] = (rot - mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF]) / 1.5f
        mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF] = rot

        // determine delta rotation around y-axis
        rot = 0f
        m = x * x + z * z
        ml = lx * lx + lz * lz
        if (m > EPSILON_ACCEL && ml > EPSILON_ACCEL) {
            m = Math.sqrt(m.toDouble()).toFloat()
            ml = Math.sqrt(ml.toDouble()).toFloat()
            var cos = x / m * (-lz / ml) + z / m * (lx / ml)
            if (cos > 1) {
                cos = 1f
            } else if (cos < -1) {
                cos = -1f
            }
            rot = 90.0f - Math.acos(cos.toDouble()).toFloat() / GlMath.PI * 180.0f
        }
        // integrate rotation
        mAccelSensorRotation[ACCEL_ROT_OFF + 1] = (mAccelSensorRotation[ACCEL_ROT_OFF + 1] + rot) * (1 - PULL_BACK * 2)
        // filter integrated rotation
        rot = mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF + 1] * 0.85f + mAccelSensorRotation[ACCEL_ROT_OFF + 1] * 0.15f
        // compute delta of integrated rotation
        mSensorRotation[1] = (rot - mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF + 1]) / 1.5f
        mAccelSensorRotation[ACCEL_FILTERED_ROT_OFF + 1] = rot
    }

    private fun integrateSensorRotation(dT: Float) {
        val rotX = mSensorRotation[0]
        val rotY = mSensorRotation[1]
        val rotZ = mSensorRotation[2]

        // integrate object rotation
        mSensorRotation[0] *= mIntensity
        mSensorRotation[1] *= mIntensity
        mSensorRotation[2] *= mIntensity
        integrateRotationMatrix(mSensorRotation, dT, ROT_CAMERA_OFF, MAX_SIN)

        // integrate light rotation
        mSensorRotation[0] = rotX * mIntensityLight
        mSensorRotation[1] = rotY * mIntensityLight
        mSensorRotation[2] = rotZ * mIntensityLight
        integrateRotationMatrix(mSensorRotation, dT, ROT_LIGHT_OFF, MAX_SIN_LIGHT)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // good to know, but actually we don't care
    }

    private fun considerDisplayRotation(sample: FloatArray, off: Int) {
        val f: Float
        when (mDisplay.rotation) {
            Surface.ROTATION_90 -> {
                f = sample[off + 1]
                sample[off + 1] = sample[off]
                sample[off] = -f
            }
            Surface.ROTATION_180 -> {
                sample[off + 1] = -sample[off + 1]
                sample[off] = -sample[off]
            }
            Surface.ROTATION_270 -> {
                f = sample[off + 1]
                sample[off + 1] = -sample[off]
                sample[off] = f
            }
        }

        val x = sample[off];
        val y = sample[off + 1];
        val c = Math.cos(-rotation.toDouble()).toFloat();
        val s = Math.sin(-rotation.toDouble()).toFloat();

        sample[off] = x * c + y * -s
        sample[off + 1] = x * s + y * c

    }

    private fun integrateRotationMatrix(deltaAxis: FloatArray, dT: Float, destOff: Int, maxSin: Float) {
        var axisX = deltaAxis[0]
        var axisY = deltaAxis[1]
        var axisZ = deltaAxis[2]

        // Calculate the angular speed of the sample
        val omegaMagnitude = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ.toDouble()).toFloat()

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            axisX /= omegaMagnitude
            axisY /= omegaMagnitude
            axisZ /= omegaMagnitude
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        val thetaOverTwo = omegaMagnitude * dT / 2.0f
        val sinThetaOverTwo = Math.sin(thetaOverTwo.toDouble()).toFloat()
        val cosThetaOverTwo = Math.cos(thetaOverTwo.toDouble()).toFloat()
        mDeltaRotationVector[0] = sinThetaOverTwo * axisX
        mDeltaRotationVector[1] = sinThetaOverTwo * axisY
        mDeltaRotationVector[2] = sinThetaOverTwo * axisZ
        mDeltaRotationVector[3] = cosThetaOverTwo

        // compute 3x3 delta rotation matrix and convert it to 4x4
        SensorManager.getRotationMatrixFromVector(mDeltaRotationMatrix, mDeltaRotationVector)
        convert3x3to4x4(mDeltaRotationMatrix, mRotationMatrix, ROT_DELTA_OFF)

        // integrate delta rotation to rotation matrix
        Matrix.multiplyMM(mRotationMatrix, ROT_TEMP_OFF, mRotationMatrix, destOff, mRotationMatrix, ROT_DELTA_OFF)
        synchronized (mRotationMatrix) {
            System.arraycopy(mRotationMatrix, ROT_TEMP_OFF, mRotationMatrix, destOff, 16)
            filterOutMatrix(destOff, maxSin)
        }
    }

    private fun convert3x3to4x4(m3x3: FloatArray, m4x4: FloatArray, off: Int) {
        m4x4[off] = m3x3[0]
        m4x4[off + 1] = m3x3[1]
        m4x4[off + 2] = m3x3[2]
        m4x4[off + 3] = 0f
        m4x4[off + 4] = m3x3[3]
        m4x4[off + 5] = m3x3[4]
        m4x4[off + 6] = m3x3[5]
        m4x4[off + 7] = 0f
        m4x4[off + 8] = m3x3[6]
        m4x4[off + 9] = m3x3[7]
        m4x4[off + 10] = m3x3[8]
        m4x4[off + 11] = 0f
        m4x4[off + 12] = 0f
        m4x4[off + 13] = 0f
        m4x4[off + 14] = 0f
        m4x4[off + 15] = 1f
    }

    private fun filterOutMatrix(destOff: Int, maxSin: Float) {
        // slowly pull matrix to identity
        val f0 = 1 - PULL_BACK
        mRotationMatrix[destOff] = mRotationMatrix[destOff] * f0 + PULL_BACK
        mRotationMatrix[destOff + 5] = mRotationMatrix[destOff + 5] * f0 + PULL_BACK
        mRotationMatrix[destOff + 10] = mRotationMatrix[destOff + 10] * f0 + PULL_BACK

        mRotationMatrix[destOff + 1] *= f0
        mRotationMatrix[destOff + 2] *= f0
        mRotationMatrix[destOff + 4] *= f0
        mRotationMatrix[destOff + 6] *= f0
        mRotationMatrix[destOff + 8] *= f0
        mRotationMatrix[destOff + 9] *= f0

        mTestVec[0] = 0f
        mTestVec[1] = 0f
        mTestVec[2] = 1f
        transformVec3(mTestVec, mRotationMatrix, destOff)
        val sinx = mTestVec[0]
        val siny = mTestVec[1]
        if (sinx > maxSin) {
            val a = (Math.asin(sinx.toDouble()) - Math.asin(maxSin.toDouble())).toFloat() / GlMath.PI * 180
            Matrix.rotateM(mRotationMatrix, destOff, -a, 0f, 1f, 0f)
        } else if (sinx < -maxSin) {
            val a = (Math.asin(sinx.toDouble()) - Math.asin((-maxSin).toDouble())).toFloat() / GlMath.PI * 180
            Matrix.rotateM(mRotationMatrix, destOff, -a, 0f, 1f, 0f)
        }
        if (siny > maxSin) {
            val a = (Math.asin(siny.toDouble()) - Math.asin(maxSin.toDouble())).toFloat() / GlMath.PI * 180
            Matrix.rotateM(mRotationMatrix, destOff, a, 1f, 0f, 0f)
        } else if (siny < -maxSin) {
            val a = (Math.asin(siny.toDouble()) - Math.asin((-maxSin).toDouble())).toFloat() / GlMath.PI * 180
            Matrix.rotateM(mRotationMatrix, destOff, a, 1f, 0f, 0f)
        }

        // ortho-normalize rotation matrix
        // normalize X
        GlMath.normalize(mRotationMatrix, destOff)
        // Z = X x Y
        cross(mRotationMatrix, destOff + 8, mRotationMatrix, destOff, mRotationMatrix, destOff + 4)
        GlMath.normalize(mRotationMatrix, destOff + 8)
        // Y = Z x X
        cross(mRotationMatrix, destOff + 4, mRotationMatrix, destOff + 8, mRotationMatrix, destOff)
    }

    private fun cross(result: FloatArray, resultOff: Int, lhs: FloatArray, lhsOff: Int, rhs: FloatArray, rhsOff: Int) {
        result[resultOff] = lhs[lhsOff + 1] * rhs[rhsOff + 2] - lhs[lhsOff + 2] * rhs[rhsOff + 1]
        result[resultOff + 1] = lhs[lhsOff + 2] * rhs[rhsOff] - lhs[lhsOff] * rhs[rhsOff + 2]
        result[resultOff + 2] = lhs[lhsOff] * rhs[rhsOff + 1] - lhs[lhsOff + 1] * rhs[rhsOff]
    }

    private fun transformVec3(vec3: FloatArray, mat: FloatArray, matOff: Int) {
        val x = vec3[0] * mat[matOff] + vec3[1] * mat[matOff + 4] + vec3[2] * mat[matOff + 8]
        val y = vec3[0] * mat[matOff + 1] + vec3[1] * mat[matOff + 5] + vec3[2] * mat[matOff + 9]
        val z = vec3[0] * mat[matOff + 2] + vec3[1] * mat[matOff + 6] + vec3[2] * mat[matOff + 10]
        vec3[0] = x
        vec3[1] = y
        vec3[2] = z
    }

    companion object {

        private val TAG = "ParallaxHelper"

        val NO_ROT_AVAILABLE = 0
        val GYRO_AVAILABLE = 1
        val ACCEL_AVAILABLE = 2

        private val EPSILON = 0.001f
        private val EPSILON_ACCEL = 2.0f
        private val MAX_SIN = 0.2f
        private val MAX_SIN_LIGHT = 0.7f
        private val PULL_BACK = 0.005f

        // offsets for mRotationMatrix
        private val ROT_CAMERA_OFF = 0
        private val ROT_LIGHT_OFF = 16
        private val ROT_DELTA_OFF = 32
        private val ROT_TEMP_OFF = 48

        // offsets for acceleration sensor based rotation
        private val ACCEL_ROT_OFF = 0
        private val ACCEL_FILTERED_ROT_OFF = 3
        private val ACCEL_CURRENT_VEC_OFF = 6
        private val ACCEL_LAST_VEC_OFF = 9

        fun availableRotationMode(context: Context): Int {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                return GYRO_AVAILABLE
            } else if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                return ACCEL_AVAILABLE
            } else {
                return NO_ROT_AVAILABLE
            }
        }
    }

}
