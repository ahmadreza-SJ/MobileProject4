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
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*


class MainActivity : AppCompatActivity() {


    private lateinit var locationViewModel: LocationViewModel
    private var current_location: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var scan_delay: Long = 1000 * 60

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
                            upspeed()
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
        var lac: String = ""
        var arfcn: String = ""
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
                cid = cellIdentityGsm.cid.toString()
                mcc = cellIdentityGsm.mccString.toString()
                mnc = cellIdentityGsm.mncString.toString()
                lac = cellIdentityGsm.lac.toString()
                arfcn = cellIdentityGsm.getArfcn().toString()
                cell_type = "GSM"
            }
            if (cellInfo is CellInfoWcdma) {
                val cellIdentityWcdma: CellIdentityWcdma = cellInfo.cellIdentity
                cid = cellIdentityWcdma.cid.toString()
                mcc = cellIdentityWcdma.mccString.toString()
                mnc = cellIdentityWcdma.mncString.toString()
                lac = cellIdentityWcdma.lac.toString()
                arfcn = cellIdentityWcdma.uarfcn.toString()
                cell_type = "UMTS"
            }
            if (cellInfo is CellInfoLte) {
                val cellIdentityLte: CellIdentityLte = cellInfo.cellIdentity
                cid = cellIdentityLte.ci.toString()
                mcc = cellIdentityLte.mccString.toString()
                mnc = cellIdentityLte.mncString.toString()
                lac = cellIdentityLte.tac.toString()
                arfcn = cellIdentityLte.earfcn.toString()
                cell_type = "LTE"
            }
        }
        catch (e: IndexOutOfBoundsException) {
            Toast.makeText(this@MainActivity, "No Signal", Toast.LENGTH_SHORT).show()
        }
        finally {
            requestNewLocationData()
            if (current_location != null )
            {
//                Log.d("ADebugTag", "longitude Value: " + current_location!!.longitude);
//                Log.d("ADebugTag", "latitude Value: " + current_location!!.latitude);
                val cellData = Cell(
                    cid = cid,
                    lac_tac = lac,
                    mcc = mcc,
                    mnc = mnc,
                    arfcn = arfcn,
                    cellType = cell_type
                )
                val location = LocData(
                    id = null,
                    latitude = current_location!!.latitude,
                    longitude = current_location!!.longitude,
                    cellId = cellData.cid,
                    time = System.currentTimeMillis(),
                )
                var checked : Boolean = false
                var dist_constraint : Boolean = false
                locationViewModel.allLocations.observe(this, Observer { locations ->
                    // Update the list of markers
                    Log.d("ADebugTag", "Finding each location");
                    locations?.let {
                        if (locations != null) {

                            checked = true
                            val distances = floatArrayOf(.1f)
                            for (oldlocation in locations.iterator()) {
                                Location.distanceBetween(
                                    oldlocation.latitude, oldlocation.longitude,
                                    location.latitude, location.longitude, distances
                                );
                            }
                            Log.d("ADebugTag", "distance location! " + distances[0].toString());
                            if (distances[0] < 3) {
                                Log.d(
                                    "ADebugTag",
                                    "Dont add new locatiob " + distances[0].toString()
                                );
                                dist_constraint = true
                            }
                        }
                    }
                })
                if (checked == false || dist_constraint == false){
                    Log.d("ADebugTag", "Location added");
                    locationViewModel.addCell(cellData)
                    locationViewModel.addLocation(location)
                }


            }

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

    var startTime: Long = 0
    var endTime: Long = 0
    var fileSize: Int = 0
    var client: OkHttpClient = OkHttpClient()

    // bandwidth in kbps
    private val POOR_BANDWIDTH = 150
    private val AVERAGE_BANDWIDTH = 550
    private val GOOD_BANDWIDTH = 2000

    private fun speed(){
        val request: Request = Request.Builder()
            .url("https://publicobject.com/helloworld.txt")
            .build()

        startTime = System.currentTimeMillis()

        client.newCall(request).enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful()) throw IOException("Unexpected code $response")
                val responseHeaders: Headers = response.headers()
                var i = 0
                val size: Int = responseHeaders.size()
                while (i < size) {
                    println(responseHeaders.name(i).toString() + ": " + responseHeaders.value(i))
                    i++
                }
                val input: InputStream? = response.body()?.byteStream()
                fileSize = try {
                    val bos = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    while (input?.read(buffer) !== -1) {
                        bos.write(buffer)
                    }
                    val docBuffer: ByteArray = bos.toByteArray()
                    bos.size()
                } finally {
                    input?.close()
                }
                endTime = System.currentTimeMillis()

                val timeTakenMills =
                    Math.floor((endTime - startTime).toDouble()) // time taken in milliseconds
                val timeTakenSecs = timeTakenMills / 1000 // divide by 1000 to get time in seconds
                val kilobytePerSec = Math.round(1024 / timeTakenSecs).toInt()
                println("kilobyte per sec: $kilobytePerSec")
            }

        })

    }


    fun upspeed() {
//        try {
//        https://api.imgbb.com/1/upload?key=8deb481db621c460ddaac584c5665308&image=PCFET0NUWVBFIEhUTUwgUFVCTElDICItLy9JRVRGLy9EVEQgSFRNTCAyLjAvL0VOIj4KPGh0bWw+PGhlYWQ+Cjx0aXRsZT4zMDEgTW92ZWQgUGVybWFuZW50bHk8L3RpdGxlPgo8L2hlYWQ+PGJvZHk+CjxoMT5Nb3ZlZCBQZXJtYW5lbnRseTwvaDE+CjxwPlRoZSBkb2N1bWVudCBoYXMgbW92ZWQgPGEgaHJlZj0iaHR0cDovL3NjYWxld2F5LnRlc3RkZWJpdC5pbmZvLyI+aGVyZTwvYT4uPC9wPgo8L2JvZHk+PC9odG1sPgo=
           val jsonObject = JSONObject()
            jsonObject.put(
                "image",
                "PCFET0NUWVBFIEhUTUwgUFVCTElDICItLy9JRVRGLy9EVEQgSFRNTCAyLjAvL0VOIj4KPGh0bWw+PGhlYWQ+Cjx0aXRsZT4zMDEgTW92ZWQgUGVybWFuZW50bHk8L3RpdGxlPgo8L2hlYWQ+PGJvZHk+CjxoMT5Nb3ZlZCBQZXJtYW5lbnRseTwvaDE+CjxwPlRoZSBkb2N1bWVudCBoYXMgbW92ZWQgPGEgaHJlZj0iaHR0cDovL3NjYWxld2F5LnRlc3RkZWJpdC5pbmZvLyI+aGVyZTwvYT4uPC9wPgo8L2JvZHk+PC9odG1sPgo="
            )
            jsonObject.put("key", "8deb481db621c460ddaac584c5665308")
            val JSON = MediaType.parse("application/json; charset=utf-8")
            val body = RequestBody.create(JSON, jsonObject.toString())
    //            val requestBody: RequestBody = MultipartBody.Builder().setType(MultipartBody.)
    //                .addFormDataPart(
    //                    "image", file.getName(),
    //                    RequestBody.create(MediaType.parse("image/jpg"), file)
    //                )
    //                .build()
        val requestBody : RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("imgage","PCFET0NUWVBFIEhUTUwgUFVCTElDICItLy9JRVRGLy9EVEQgSFRNTCAyLjAvL0VOIj4KPGh0bWw+PGhlYWQ+Cjx0aXRsZT4zMDEgTW92ZWQgUGVybWFuZW50bHk8L3RpdGxlPgo8L2hlYWQ+PGJvZHk+CjxoMT5Nb3ZlZCBQZXJtYW5lbnRseTwvaDE+CjxwPlRoZSBkb2N1bWVudCBoYXMgbW92ZWQgPGEgaHJlZj0iaHR0cDovL3NjYWxld2F5LnRlc3RkZWJpdC5pbmZvLyI+aGVyZTwvYT4uPC9wPgo8L2JvZHk+PC9odG1sPgo=")
            .build()
            println("upload create body.....${requestBody.contentLength()}")
            val request: Request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=8deb481db621c460ddaac584c5665308&image=PCFET0NUWVBFIEhUTUwgUFVCTElDICItLy9JRVRGLy9EVEQgSFRNTCAyLjAvL0VOIj4KPGh0bWw+PGhlYWQ+Cjx0aXRsZT4zMDEgTW92ZWQgUGVybWFuZW50bHk8L3RpdGxlPgo8L2hlYWQ+PGJvZHk+CjxoMT5Nb3ZlZCBQZXJtYW5lbnRseTwvaDE+CjxwPlRoZSBkb2N1bWVudCBoYXMgbW92ZWQgPGEgaHJlZj0iaHR0cDovL3NjYWxld2F5LnRlc3RkZWJpdC5pbmZvLyI+aGVyZTwvYT4uPC9wPgo8L2JvZHk+PC9odG1sPgo=")
                .get()
                .build()
            println("after request.....")
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    println("fail to upload")
                    if (e != null) {
                        e.printStackTrace()
                    }
                }

                override fun onResponse(call: Call?, response: Response) {
                    if (!response.isSuccessful) {
                        println("fail to upload unsuccess.....$response")
                        // Handle the error
                    }
                    endTime = System.currentTimeMillis()
                    val timeTakenMills =
                        Math.floor((endTime - startTime).toDouble()) // time taken in milliseconds
                    val timeTakenSecs =
                        timeTakenMills / 1000 // divide by 1000 to get time in seconds
                    val kilobytePerSec = Math.round(1024 * 5 / timeTakenSecs).toInt()
                    println("upload upload kilobyte per sec: $kilobytePerSec")
                }
            })
//        } catch (ex: Exception) {
//            println("upload infinal cash.....")
//
//            // Handle the error
//        }
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