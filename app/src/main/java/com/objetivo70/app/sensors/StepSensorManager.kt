package com.objetivo70.app.sensors

import android.content.Context
import android.hardware.*
import java.time.LocalDate

class StepSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("step_sensor", Context.MODE_PRIVATE)
    var onSteps: ((Int) -> Unit)? = null

    val available: Boolean get() = sensor != null

    fun start() { sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) } }
    fun stop() { sensorManager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values.firstOrNull()?.toInt() ?: return
        val day = LocalDate.now().toString()
        val savedDay = prefs.getString("day", null)
        var base = prefs.getInt("base", total)
        if (savedDay != day) {
            base = total
            prefs.edit().putString("day", day).putInt("base", base).apply()
        }
        onSteps?.invoke((total - base).coerceAtLeast(0))
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
