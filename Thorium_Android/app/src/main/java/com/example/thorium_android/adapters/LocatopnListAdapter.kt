package com.example.thorium_android.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.thorium_android.R
import com.example.thorium_android.entities.Cell
import com.example.thorium_android.entities.LocData
import kotlinx.android.synthetic.main.trace_recyclerview_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class LocatopnListAdapter :
    ListAdapter<Pair<LocData, Cell>, LocatopnListAdapter.MyViewHolder>(DiffUtil) {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.trace_recyclerview_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = currentList[position].first
        val cell = currentList[position].second
        holder.itemView.type.text = cell!!.cellType
        holder.itemView.time.text = getDate(currentItem.time, "dd/MM/yyyy hh:mm:ss.SSS")
        holder.itemView.latitude.text = "Lat: " +  currentItem.latitude.toString()
        holder.itemView.longitude.text = "Long: " + currentItem.longitude.toString()
        holder.itemView.cid.text = "CID: " + cell!!.cid
        holder.itemView.rssi.text = "RSSI: " + cell!!.rssi
        holder.itemView.rsrp.text = "RSRP: " + cell!!.rsrp
        holder.itemView.rsrq.text = "RSRQ: " + cell!!.rsrq
        holder.itemView.level.text = "Level: " + cell!!.level
        holder.itemView.jitter.text = "Jitter: " + cell!!.jitter
        holder.itemView.ecn0.text = "ECN0: " + cell!!.ecn0
        holder.itemView.cpich.text = "CPICH: " + cell!!.cpich
        holder.itemView.uplink.text = "Up Link Speed: " + cell!!.upKiloBytePerSec
        holder.itemView.downlink.text = "Down Link Speed: " + cell!!.downKiloBytePerSec
        holder.itemView.avg_latancy.text = "Avg. Latancy: " + cell!!.avgLatency
        holder.itemView.plmn.text = "PLMN: " + cell!!.plmn
    }
    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }
}

object DiffUtil : DiffUtil.ItemCallback<Pair<LocData, Cell>>() {
    override fun areContentsTheSame(
        oldItem: Pair<LocData, Cell>,
        newItem: Pair<LocData, Cell>
    ): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(
        oldItem: Pair<LocData, Cell>,
        newItem: Pair<LocData, Cell>
    ): Boolean {
        return oldItem == newItem
    }
}