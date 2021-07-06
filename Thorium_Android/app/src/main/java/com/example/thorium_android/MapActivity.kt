package com.example.thorium_android

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.telephony.CellInfo
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.thorium_android.entities.Cell
import com.example.thorium_android.entities.LocData
import com.example.thorium_android.entities.relations.CellWithLocations
import com.example.thorium_android.view_models.LocationViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.trace_recyclerview_item.view.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationViewModel: LocationViewModel
    private var activeMarkers = mutableListOf<Marker>()
    inner class NetCell(var cell: Cell, val location: LocData)
    var color_map = mutableMapOf<Int, Int>()
    var next_color : Int = 0
    private val filters =
        arrayOf("CID", "LAC/TAC", "Cell Type", "MCC", "MNC","PLMN", "ARFCN")
    var color_method = "CID"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_map)
        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)

        val spinner: Spinner = findViewById(R.id.spinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                next_color = 0
                color_map.clear()
                color_method = spinner.selectedItem.toString()
                updateMarkers()

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                next_color = 0
                color_map.clear()
                color_method = "CID"
                updateMarkers()

            }
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }
            override fun getInfoContents(marker: Marker): View {
                val info = LinearLayout(applicationContext)
                info.orientation = LinearLayout.VERTICAL
                val title = TextView(applicationContext)
                title.setTextColor(Color.BLACK)
                title.gravity = Gravity.CENTER
                title.setTypeface(null, Typeface.BOLD)
                title.text = marker.title
                val snippet = TextView(applicationContext)
                snippet.setTextColor(Color.GRAY)
                snippet.text = marker.snippet
                info.addView(title)
                info.addView(snippet)
                return info
            }
            
        })
        locationViewModel.allCellWithLocations.observe(this, Observer { allCells ->
            // Update the list of markers
            allCells?.let { showAllStatioMarkers(allCells, color_method) }
        })


    }

    fun updateMarkers(){
        locationViewModel.allCellWithLocations.observe(this, Observer { allCells ->
            // Update the list of markers
            allCells?.let { showAllStatioMarkers(allCells, color_method) }
        })
    }

    fun showAllStatioMarkers(allCells: List<CellWithLocations>, coloring_method: String) {

        for(marker in activeMarkers) {
            marker.remove()
        }

        val cellMap = ArrayList<NetCell>()
        if (allCells != null) {
            for (data in allCells.iterator()) {
                val locations = data.locData
                for (location in locations) {
                        cellMap.add(NetCell(data.cell,location))
                }
            }
        }

        cellMap.forEachIndexed { index, thecell ->
            val cid: Double = thecell.cell.cid.toDouble()
            val x = thecell.location.latitude
            val y = thecell.location.longitude
            val pos = LatLng(x, y)
            val color = getColorz(thecell.cell,coloring_method)
            val plmn = thecell.cell.mcc + thecell.cell.mnc
            val lac = thecell.cell.lac_tac
            val celtype = thecell.cell.cellType

            val snip = "$celtype \n Cell: $cid \n PLMN $plmn \n LAC $lac"
            val marker = mMap.addMarker(MarkerOptions().icon(
                BitmapDescriptorFactory.defaultMarker(color)).position(
                pos).title(cid.toString()).snippet(snip))
            activeMarkers.add(marker)
        }
    }

        val cid: Double = the_cell.cell.cid.toDouble()
        val x = the_cell.location.latitude
        val y = the_cell.location.longitude
        val pos = LatLng(x, y)
        val color = getColorz(the_cell.cell,coloring_method)
        val plmn = the_cell.cell.mcc + the_cell .cell.mnc
        val lac = the_cell.cell.lac_tac
        val celtype = the_cell.cell.cellType

        val snip = "$celtype \n Cell: $cid \n PLMN $plmn \n LAC $lac"
        val marker = mMap.addMarker(MarkerOptions().icon(
            BitmapDescriptorFactory.defaultMarker(color)).position(
            pos).title(cid.toString()).snippet(snip))
        activeMarkers.add(marker)
        Log.d("ADebugTag", "After new point adding");
    }

    fun getColorz(cell: Cell, coloring_method: String): Float {
        val colors = listOf(
                BitmapDescriptorFactory.HUE_AZURE,
                BitmapDescriptorFactory.HUE_GREEN,
                BitmapDescriptorFactory.HUE_ORANGE,
                BitmapDescriptorFactory.HUE_ROSE,
                BitmapDescriptorFactory.HUE_YELLOW,
                BitmapDescriptorFactory.HUE_BLUE,
                BitmapDescriptorFactory.HUE_CYAN,
                BitmapDescriptorFactory.HUE_MAGENTA,
                BitmapDescriptorFactory.HUE_RED,
                BitmapDescriptorFactory.HUE_VIOLET
        )
        var len_colors = 10
        var param = 0
        if (coloring_method == "CID"){
            param = cell.cid.toInt()
        }
        else if(coloring_method == "Cell Type"){
            val cell_type = cell.cellType
            if(cell_type == "GSM"){//blue
                return BitmapDescriptorFactory.HUE_BLUE
            }else if(cell_type=="UMTS"){//yellow
                return BitmapDescriptorFactory.HUE_YELLOW
            }else if (cell_type=="LTE"){//orange
                return BitmapDescriptorFactory.HUE_ORANGE
            }else{//reg
                return BitmapDescriptorFactory.HUE_RED
            }
        }
        else if(coloring_method=="PLMN"){
            param = (cell.mcc+cell.mnc).toInt()
        }
        else if(coloring_method=="MNC"){
            param = cell.mnc.toInt()
        }
        else if(coloring_method=="MCC"){
            param = cell.mcc.toInt()
        }
        else if(coloring_method=="LAC/TAC"){
            param = cell.lac_tac.toInt()
        }
        else if (coloring_method == "ARFCN"){
            param = cell.arfcn.toInt()
        }
        var cur_color = 0
        if (color_map.containsKey(param)){
            cur_color = color_map[param]!!

        } else {
            color_map.put(param, next_color)
            val cur_color = next_color
            next_color = (next_color + 1)%len_colors
        }
        return colors[cur_color];
    }
}