package com.example.thorium_android.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Cell(

    @PrimaryKey(autoGenerate = false) val cid: String,
    val plmn: String,
    val rssi: String,
    val level: String,
    val rsrp: String,
    val rsrq: String,
    val ecn0: String,
    val cpich: String,
    val cellType: String,
    val jitter: String,
    val avgLatency: String,
    val downKiloBytePerSec: String,
    val upKiloBytePerSec: String,
)