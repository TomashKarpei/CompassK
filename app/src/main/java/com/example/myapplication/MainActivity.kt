package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
//import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), LocationListener, SensorEventListener {


    val REQUEST_LOCATION = 2

    private var  sensorManager: SensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setLocation()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startCompass()
    }

    private fun setLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION)
        }else{
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            val provider = locationManager.getBestProvider(criteria, false)
            val location = locationManager.getLastKnownLocation(provider.toString())

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
            if(location != null){
                val textIdLoc = findViewById<TextView>(R.id.text_view_location)
                textIdLoc.text = convertLocationToString(location.latitude, location.longitude)
            }
            else{
                Toast.makeText(this, "Location is not available! ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_LOCATION) setLocation()
    }

    private fun convertLocationToString(latitude: Double, longitude: Double): String {

        val builder = StringBuilder()
        if (latitude < 0) builder.append("S ") else builder.append("N ")

        val latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS)
        val latitudeSplit = latitudeDegrees.split((":").toRegex()).dropLastWhile ({ it.isEmpty() }).toTypedArray()
        builder.append(latitudeSplit[0])
        builder.append("*")
        builder.append(latitudeSplit[1])
        builder.append("'")
        builder.append(latitudeSplit[2])
        builder.append("\"")
        builder.append("\n")

        if (longitude < 0) builder.append("W ") else builder.append("E ")
        val longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS)
        val longitudeSplit = longitudeDegrees.split((":").toRegex()).dropLastWhile({it.isEmpty()}).toTypedArray()
        builder.append(longitudeSplit[0])
        builder.append("*")
        builder.append(longitudeSplit[1])
        builder.append("'")
        builder.append(longitudeSplit[2])
        builder.append("\"")
        builder.append("\n")

        return builder.toString()
    }

    override fun onLocationChanged(p0: Location) {
        setLocation()
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

    }

    override fun onProviderEnabled(p0: String) {

    }

    override fun onProviderDisabled(p0: String) {

    }

    private var rotationMatrix = FloatArray(9)

    private var orientation = FloatArray(3)

    private var azimuth: Int = 0

    private var lastAccelerometer = FloatArray(3)

    private var lastAccelerometerSet = false

    private var lastMagnetometer = FloatArray(3)

    private var lastMagnetometerSet = false

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_ROTATION_VECTOR){
            SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values)
            azimuth = (Math.toDegrees(SensorManager.getOrientation(rotationMatrix,orientation)[0].toDouble())+360).toInt()%360
        }

        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            System.arraycopy(event.values,0,lastAccelerometer,0,event.values.size)
            lastAccelerometerSet = true
        }else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(event.values,0,lastMagnetometer,0,event.values.size)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet){
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0].toDouble())+360).toInt()%360
        }

        azimuth = Math.round(azimuth.toFloat())

        val imageIdArrow = findViewById<ImageView>(R.id.comapss_arrow)
        imageIdArrow.rotation = (-azimuth).toFloat()

        val where = when(azimuth){
            in 281..349 -> "NW"
            in 261.. 280 -> "W"
            in 191..260 -> "SW"
            in 171..190 -> "S"
            in 101..170 -> "SE"
            in 81..100 -> "E"
            in 11..80 -> "NE"
            else -> "N"
        }
        val textIdDegree = findViewById<TextView>(R.id.text_view_degree)
        textIdDegree.text = "$azimuth* $where"

    }

    override fun onAccuracyChanged(Sensor: Sensor?, p1: Int) {

    }

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var haveSensorAccelerometer = false
    private var haveSensorMagnetometer = false
    private var rotationVector: Sensor? = null
    private var haveSensorRotationVector = false

    private fun startCompass(){
        if(sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null){
            if (sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null
                || sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null){
                noSensorAlert()
            }else{
                accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                magnetometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

                haveSensorAccelerometer = sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
                haveSensorMagnetometer = sensorManager!!.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }else{
            rotationVector = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            haveSensorRotationVector = sensorManager!!.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopCompass(){
        if(haveSensorRotationVector) sensorManager!!.unregisterListener(this, rotationVector)
        if(haveSensorAccelerometer) sensorManager!!.unregisterListener(this, accelerometer)
        if(haveSensorMagnetometer) sensorManager!!.unregisterListener(this, magnetometer)
    }
    private fun noSensorAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage("Your device doesn't support a compass!")
            .setCancelable(false)
            .setNegativeButton("Close"){
                _,_ -> finish()
            }
        alertDialog.show()
    }

    override fun onResume() {
        super.onResume()
        startCompass()
    }

    override fun onPause() {
        super.onPause()
        stopCompass()
    }
}




























