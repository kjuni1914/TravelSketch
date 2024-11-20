package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Box(
    @PropertyName("box_x") val boxX: Int = 0,
    @PropertyName("box_y") val boxY: Int = 0,
    @PropertyName("box_z") val boxZ: Int = 0,
    @PropertyName("data") val data: String = "",
    @PropertyName("degree") val degree: Int = 0,
    @PropertyName("height") val height: Int = 0,
    @PropertyName("latitude") val latitude: Double? = null,
    @PropertyName("longitude") val longitude: Double? = null,
    @PropertyName("time") val time: Int? = null,
    @PropertyName("type") val typeString: String,
    @PropertyName("width") val width: Int = 0
) {
    val type: BoxType
        get() = BoxType.fromString(typeString)
}