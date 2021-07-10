package com.example.thorium_android.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.thorium_android.entities.Cell
import com.example.thorium_android.entities.LocData
import com.example.thorium_android.entities.relations.CellWithLocations
import com.example.thorium_android.view_models.LocationViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions



class MapHelpers {


    class NetCell(var cell: Cell, val location: LocData)


    companion object {

        var next_color : Int = 0

        fun updateMarkers(locationViewModel: LocationViewModel,
                          owner: LifecycleOwner,
                          color_method: String,
                          activeMarkers: MutableList<Marker>,
                          mMap: GoogleMap,
                          color_map: MutableMap<Int, Int>
        ){
            locationViewModel.allCellWithLocations.observe(owner, Observer { allCells ->
                // Update the list of markers
                allCells?.let { showAllStatioMarkers(allCells, color_method, activeMarkers, mMap, color_map) }
            })
        }

        fun showAllStatioMarkers(allCells: List<CellWithLocations>,
                                 coloring_method: String, activeMarkers: MutableList<Marker>,
                                 mMap: GoogleMap,
                                 color_map: MutableMap<Int, Int>

        ) {

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
                val color = getColorz(thecell.cell,coloring_method, color_map)
                val plmn = thecell.cell.mcc + thecell.cell.mnc
                val lac = thecell.cell.lac_tac
                val celtype = thecell.cell.cellType

                val snip = "$celtype \n Cell: $cid \n PLMN $plmn \n LAC $lac"
                val marker = mMap.addMarker(
                    MarkerOptions().icon(
                    BitmapDescriptorFactory.defaultMarker(color)).position(
                    pos).title(cid.toString()).snippet(snip))
                activeMarkers.add(marker)
            }
        }

        fun getColorz(cell: Cell, coloring_method: String, color_map: MutableMap<Int, Int>): Float {
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
}