package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class CanvasData(
    val title: String = "",
    @get:PropertyName("avg_latitude") @set:PropertyName("avg_latitude") var avgLatitude: Double? = null,
    @get:PropertyName("avg_longitude") @set:PropertyName("avg_longitude") var avgLongitude: Double? = null,
    @get:PropertyName("is_visible") @set:PropertyName("is_visible") var isVisible: Boolean = true,
    @get:PropertyName("preview_box_id") @set:PropertyName("preview_box_id") var previewBoxId: String? = null,
    val range: Double = .0
) {
    override fun toString(): String {
        return "Canvas(avgLatitude=$avgLatitude, avgLongitude=$avgLongitude, isVisible=$isVisible, previewBoxId=$previewBoxId, range=$range)"
    }
}
