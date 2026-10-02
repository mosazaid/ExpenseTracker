package com.example.expensetracker.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassReading(
    val azimuth: Float = 0f, // Device azimuth in degrees (0..360, where 0 is North)
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSensorAvailable: Boolean = true,
    val pitch: Float = 0f,
    val roll: Float = 0f
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
    private val orientationAngles = FloatArray(3)

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var smoothedAzimuth = 0f
    private var isListening = false

    fun start() {
        if (isListening) return
        val hasRotationVector = rotationVectorSensor != null
        if (hasRotationVector) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
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
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val normalizedAzimuth = (rawAzimuth + 360f) % 360f
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                updateAzimuth(normalizedAzimuth, event.accuracy, pitch, roll)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                gravityValues = event.values.clone()
                hasGravity = true
                if (hasGeomagnetic) computeFromAccMag(event.accuracy)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagneticValues = event.values.clone()
                hasGeomagnetic = true
                if (hasGravity) computeFromAccMag(event.accuracy)
            }
        }
    }

    private fun computeFromAccMag(accuracy: Int) {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalizedAzimuth = (rawAzimuth + 360f) % 360f
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            updateAzimuth(normalizedAzimuth, accuracy, pitch, roll)
        }
    }

    private fun updateAzimuth(newAzimuth: Float, accuracy: Int, pitch: Float, roll: Float) {
        // Smooth angle along the shortest circular path
        var diff = (newAzimuth - smoothedAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        // Exponential smoothing factor: 0.25 provides responsive yet stable rotation
        smoothedAzimuth = (smoothedAzimuth + diff * 0.25f + 360f) % 360f

        _reading.value = CompassReading(
            azimuth = smoothedAzimuth,
            accuracy = accuracy,
            isSensorAvailable = true,
            pitch = pitch,
            roll = roll
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _reading.value = _reading.value.copy(accuracy = accuracy)
    }
}
