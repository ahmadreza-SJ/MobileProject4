package com.example.thorium_android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.thorium_android.R
import com.example.thorium_android.utils.MapHelpers
import com.example.thorium_android.view_models.LocationViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker


class MapFragment2 : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView : MapView
    private lateinit var locationViewModel: LocationViewModel
    private var activeMarkers = mutableListOf<Marker>()
    var color_map = mutableMapOf<Int, Int>()
    private val filters =

        arrayOf("CID", "Cell Type","PLMN")
    var color_method = "CID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val theview =  inflater.inflate(R.layout.fragment_map2, container, false)
        return theview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("on view created..........")
        mapView = view?.findViewById(R.id.map) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.onResume();
        mapView.getMapAsync(this)
        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        val spinner: Spinner = requireView().findViewById(R.id.spinner)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        spinner.adapter = adapter
        val context = this
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                MapHelpers.next_color = 0
                color_map.clear()
                color_method = spinner.selectedItem.toString()
                MapHelpers.updateMarkers(
                    locationViewModel,
                    context,
                    color_method,
                    activeMarkers,
                    mMap,
                    color_map
                )

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                MapHelpers.next_color = 0
                color_map.clear()
                color_method = "CID"
                MapHelpers.updateMarkers(
                    locationViewModel,
                    context,
                    color_method,
                    activeMarkers,
                    mMap,
                    color_map
                )

            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        println("on map ready..........")
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val info = LinearLayout(requireContext().applicationContext)
                info.orientation = LinearLayout.VERTICAL
                val title = TextView(requireContext().applicationContext)
                title.setTextColor(Color.BLACK)
                title.gravity = Gravity.CENTER
                title.setTypeface(null, Typeface.BOLD)
                title.text = marker.title
                val snippet = TextView(requireContext().applicationContext)
                snippet.setTextColor(Color.GRAY)
                snippet.text = marker.snippet
                info.addView(title)
                info.addView(snippet)
                return info
            }

        })
        locationViewModel.allCellWithLocations.observe(this, Observer { allCells ->
            // Update the list of markers
            allCells?.let {
                MapHelpers.showAllStatioMarkers(
                    allCells,
                    color_method,
                    activeMarkers,
                    mMap,
                    color_map
                )
            }
        })
    }

}