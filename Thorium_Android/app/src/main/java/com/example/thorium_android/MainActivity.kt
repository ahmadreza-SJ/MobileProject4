package com.example.thorium_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.thorium_android.entities.Cell
import com.example.thorium_android.entities.LocData
import com.example.thorium_android.view_models.LocationViewModel
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.InetAddress
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {


    private lateinit var locationViewModel: LocationViewModel
    private var current_location: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var scan_delay: Long = 1000 * 60
    private var jitter: Float = 0f
    private var avg_latency: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

//        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }

        isLocationEnabled()
        val trace_bottun = findViewById<Button>(R.id.button_trace)
        val tm = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {

                    val mainHandler = Handler(Looper.getMainLooper())
//
                    mainHandler.post(object : Runnable {
                        override fun run() {
                            getCellInfo(tm)
                            ping()
                            mainHandler.postDelayed(this, scan_delay)
                        }
                    })
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) {
                }
            }).check()

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    fun getCellInfo(tm: TelephonyManager){

        var cid: String = ""
        var mcc: String = ""
        var mnc: String = ""
        var rssi: String = ""
        var level: String = ""
        var rsrp: String = ""
        var rsrq: String = ""
        var ecn0: String = ""
        var cpich: String = ""
        var cell_type: String = ""

        val infos = tm.allCellInfo

        if (infos.size == 0){
            Log.d("myTag", "No signal");

            Toast.makeText(this@MainActivity, "No Signal", Toast.LENGTH_SHORT).show()
        }

        try {
            val cellInfo = tm.allCellInfo[0]
            if (cellInfo is CellInfoGsm) {
                val cellIdentityGsm: CellIdentityGsm = cellInfo.cellIdentity
                val cellStrength: CellSignalStrengthGsm = cellInfo.cellSignalStrength
                cid = cellIdentityGsm.cid.toString()
                mcc = cellIdentityGsm.mccString.toString()
                mnc = cellIdentityGsm.mncString.toString()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    rssi = cellStrength.rssi.toString()
                }
                level = cellStrength.level.toString()
                cell_type = "GSM"
            }
            if (cellInfo is CellInfoWcdma) {
                val cellIdentityWcdma: CellIdentityWcdma = cellInfo.cellIdentity
                val celllStrength : CellSignalStrengthWcdma = cellInfo.cellSignalStrength
                cid = cellIdentityWcdma.cid.toString()
                mcc = cellIdentityWcdma.mccString.toString()
                mnc = cellIdentityWcdma.mncString.toString()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    ecn0 = celllStrength.ecNo.toString()
                }
                cpich = celllStrength.dbm.toString()
                level = celllStrength.level.toString()
                cell_type = "UMTS"
            }
            if (cellInfo is CellInfoLte) {
                val cellIdentityLte: CellIdentityLte = cellInfo.cellIdentity
                val celStrength : CellSignalStrengthLte = cellInfo.cellSignalStrength
                cid = cellIdentityLte.ci.toString()
                mcc = cellIdentityLte.mccString.toString()
                mnc = cellIdentityLte.mncString.toString()
                rsrp = celStrength.rsrp.toString()
                rsrq = celStrength.rsrq.toString()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    rssi = celStrength.rssi.toString()
                }
                level = celStrength.level.toString()
                cell_type = "LTE"
            }
        }
        catch (e: IndexOutOfBoundsException) {
            Toast.makeText(this@MainActivity, "No Signal", Toast.LENGTH_SHORT).show()
        }
        finally {
            requestNewLocationData()
//            if (current_location != null )
//            {
////                Log.d("ADebugTag", "longitude Value: " + current_location!!.longitude);
////                Log.d("ADebugTag", "latitude Value: " + current_location!!.latitude);
//                val cellData = Cell(
//                    cid = cid,
//                    lac_tac = lac,
//                    mcc = mcc,
//                    mnc = mnc,
//                    arfcn = arfcn,
//                    cellType = cell_type
//                )
//                val location = LocData(
//                    id = null,
//                    latitude = current_location!!.latitude,
//                    longitude = current_location!!.longitude,
//                    cellId = cellData.cid,
//                    time = System.currentTimeMillis(),
//                )
//                var checked : Boolean = false
//                var dist_constraint : Boolean = false
//                locationViewModel.allLocations.observe(this, Observer { locations ->
//                    // Update the list of markers
//                    Log.d("ADebugTag", "Finding each location");
//                    locations?.let {
//                        if (locations != null) {
//
//                            checked = true
//                            val distances = floatArrayOf(.1f)
//                            for (oldlocation in locations.iterator()) {
//                                Location.distanceBetween(
//                                    oldlocation.latitude, oldlocation.longitude,
//                                    location.latitude, location.longitude, distances
//                                );
//                            }
//                            Log.d("ADebugTag", "distance location! " + distances[0].toString());
//                            if (distances[0] < 3) {
//                                Log.d(
//                                    "ADebugTag",
//                                    "Dont add new locatiob " + distances[0].toString()
//                                );
//                                dist_constraint = true
//                            }
//                        }
//                    }
//                })
//                if (checked == false || dist_constraint == false){
//                    Log.d("ADebugTag", "Location added");
//                    locationViewModel.addCell(cellData)
//                    locationViewModel.addLocation(location)
//                }
//
//
//            }

        }
//        Log.d("ADebugTag", "cid Value: " + cid);
//        Log.d("ADebugTag", "mcc Value: " + mcc);
//        Log.d("ADebugTag", "mnc Value: " + mnc);
//        Log.d("ADebugTag", "lac Value: " + lac);
//        Log.d("ADebugTag", "cell_type Value: " + cell_type);


    }
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private fun ping() {
        var domain: String = "8.8.8.8"
        var runtime: Runtime = Runtime.getRuntime()
        var maxcount = 5
        var ipProc: Process = runtime.exec("/system/bin/ping -c $maxcount " + domain)
        var bufin = BufferedReader(InputStreamReader(ipProc.inputStream))
        var latencyResult: Float = 0f
        var counter = 0
        var tresh : Long = 10000
        var latencies = listOf<Float>()
        var start: Long = System.currentTimeMillis()
        while (counter <= maxcount) {
            var inputLine = bufin.readLine()
            if (inputLine == null || inputLine =="") {
                break
            } else {
                if(counter >0) {
                    println("...........input $inputLine")
                    latencyResult = inputLine.split("=").last().split(" ")[0].toFloat()
                    latencies = latencies + latencyResult
                    println("...........latency $latencyResult")
                }
                counter +=1
            }
            var time = System.currentTimeMillis()
            if ((time - start)> tresh ){
                println("...........opss")
                break
            }
        }
        try{
            avg_latency = latencies.sum()/latencies.size
            var diff = 0.0F
            for ((i,l) in latencies.withIndex()){
                if(i>0){
                    if(latencies[i]-latencies[i-1] > 0)
                        diff = diff + (latencies[i]-latencies[i-1])
                    else
                        diff = diff - (latencies[i]-latencies[i-1])
                }
            }
            jitter = diff/(latencies.size-1)
        }catch (e: Exception){
            jitter = -1f
            avg_latency = -1f
        }
        println("...........jitter $jitter")
        println("...........avg latency $avg_latency")
    }


    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            current_location = mLastLocation
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun showMap(view: View) {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)

    }
}