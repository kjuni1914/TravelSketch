package com.travelsketch.data.model

data class MapData(
    val canvasId: String, // 문서 이름 (canvasId)
    val avg_gps_latitude: Double = 0.0,
    val avg_gps_longitude: Double = 0.0,
    val is_visible : Boolean = false,
    val preview_box_id: String = "",
    val range : Double = 0.0,
    val title : String = ""
)
