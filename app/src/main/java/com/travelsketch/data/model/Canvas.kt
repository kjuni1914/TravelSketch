package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Canvas(
    @PropertyName("avg_latitude") val avgLatitude: Double? = null,
    @PropertyName("avg_longitude") val avgLongitude: Double? = null,
    @PropertyName("is_visible") val isVisible: Boolean = true,
    @PropertyName("preview_box_id") val previewBoxId: String? = null,
    val range: Double = .0
)
