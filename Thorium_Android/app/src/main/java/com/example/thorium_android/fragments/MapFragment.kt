package com.example.thorium_android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.thorium_android.R
import com.example.thorium_android.utils.MapHelpers
import com.example.thorium_android.view_models.LocationViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.thorium_android.utils.MapHelpers.Companion.next_color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationViewModel: LocationViewModel
    private var activeMarkers = mutableListOf<Marker>()
    var color_map = mutableMapOf<Int, Int>()
    private val filters =
        arrayOf("CID", "LAC/TAC", "Cell Type", "MCC", "MNC","PLMN", "ARFCN")
    var color_method = "CID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val spinner: Spinner = requireView().findViewById(R.id.spinner)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        spinner.adapter = adapter
        val context = this
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                next_color = 0
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
                next_color = 0
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
        return inflater.inflate(R.layout.fragment_map, container, false)
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