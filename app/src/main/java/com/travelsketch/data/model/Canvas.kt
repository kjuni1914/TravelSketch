package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Canvas(
    @PropertyName("avg_gps_latitude") val avgGpsLatitude: Double = .0,
    @PropertyName("avg_gps_longitude") val avgGpsLongitude: Double = .0,
    @PropertyName("is_visible") val isVisible: Boolean = true,
    @PropertyName("preview_box_id") val previewBoxId: Double? = null,
    val range: Double = .0
)
