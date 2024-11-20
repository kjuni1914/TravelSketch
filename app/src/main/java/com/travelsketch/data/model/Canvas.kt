package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Canvas(
    @PropertyName("avg_gps_latitude") val avgGpsLatitude: Double? = null,
    @PropertyName("avg_gps_longitude") val avgGpsLongitude: Double? = null,
    @PropertyName("is_visible") val isVisible: Boolean? = null,
    @PropertyName("preview_box_id") val previewBoxId: Double? = null,
    val range: Double? = null
)
