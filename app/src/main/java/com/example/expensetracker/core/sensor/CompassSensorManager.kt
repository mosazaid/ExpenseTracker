package com.example.expensetracker.core.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassReading(
    val azimuth: Float = 0f, // True azimuth in degrees (0..360, where 0 is True North)
    val magneticAzimuth: Float = 0f, // Raw magnetic azimuth in degrees
    val declination: Float = 0f, // Geomagnetic declination offset in degrees
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSensorAvailable: Boolean = true,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isTilted: Boolean = false,
    val isCalibrated: Boolean = true
)

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _reading = MutableStateFlow(
        CompassReading(isSensorAvailable = rotationVectorSensor != null || (accelerometerSensor != null && magneticSensor != null))
    )
    val reading: StateFlow<CompassReading> = _reading.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var magneticDeclination = 0f
    private var smoothedAzimuth = 0f
    private var isListening = false
    private var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    fun setLocation(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        try {
            val geomagneticField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitude.toFloat(),
                System.currentTimeMillis()
            )
            magneticDeclination = geomagneticField.declination
        } catch (_: Exception) {
            magneticDeclination = 0f
        }
    }

    fun start() {
        if (isListening) return
        val hasRotationVector = rotationVectorSensor != null
        if (hasRotationVector) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            // Also listen to magnetic sensor to monitor magnetic field accuracy / calibration
            magneticSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            isListening = true
        } else if (accelerometerSensor != null && magneticSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        } else {
            _reading.value = _reading.value.copy(isSensorAvailable = false)
        }
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
            currentAccuracy = event.accuracy
        }

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                computeOrientation(rotationMatrix, currentAccuracy)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                gravityValues = event.values.clone()
                hasGravity = true
                if (hasGeomagnetic) computeFromAccMag(currentAccuracy)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagneticValues = event.values.clone()
                hasGeomagnetic = true
                currentAccuracy = event.accuracy
                if (rotationVectorSensor == null && hasGravity) {
                    computeFromAccMag(currentAccuracy)
                } else {
                    _reading.value = _reading.value.copy(
                        accuracy = currentAccuracy,
                        isCalibrated = currentAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                    )
                }
            }
        }
    }

    private fun computeFromAccMag(accuracy: Int) {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)
        if (success) {
            computeOrientation(rotationMatrix, accuracy)
        }
    }

    private fun computeOrientation(rotMatrix: FloatArray, accuracy: Int) {
        SensorManager.getOrientation(rotMatrix, orientationAngles)
        val initialPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()

        // Coordinate remapping for tilted device to prevent 180° gimbal lock inversion
        val remapped = if (abs(initialPitch) > 45f) {
            val axisY = if (initialPitch > 0) SensorManager.AXIS_MINUS_Z else SensorManager.AXIS_Z
            val success = SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, axisY, remappedMatrix)
            if (success) remappedMatrix else rotMatrix
        } else {
            rotMatrix
        }

        SensorManager.getOrientation(remapped, orientationAngles)
        val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedMagAzimuth = (rawAzimuth + 360f) % 360f
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // Convert Magnetic Azimuth to True North Azimuth using Geomagnetic Declination
        val trueAzimuth = (normalizedMagAzimuth + magneticDeclination + 360f) % 360f

        updateAzimuth(trueAzimuth, normalizedMagAzimuth, accuracy, pitch, roll)
    }

    private fun updateAzimuth(
        newTrueAzimuth: Float,
        magAzimuth: Float,
        accuracy: Int,
        pitch: Float,
        roll: Float
    ) {
        // Smooth angle along the shortest circular path
        var diff = (newTrueAzimuth - smoothedAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        // Exponential smoothing factor: 0.22 provides responsive yet stable rotation
        smoothedAzimuth = (smoothedAzimuth + diff * 0.22f + 360f) % 360f

        val isTilted = abs(pitch) > 30f || abs(roll) > 30f
        val isCalibrated = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM

        _reading.value = CompassReading(
            azimuth = smoothedAzimuth,
            magneticAzimuth = magAzimuth,
            declination = magneticDeclination,
            accuracy = accuracy,
            isSensorAvailable = true,
            pitch = pitch,
            roll = roll,
            isTilted = isTilted,
            isCalibrated = isCalibrated
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = accuracy
        _reading.value = _reading.value.copy(
            accuracy = accuracy,
            isCalibrated = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        )
    }
}
