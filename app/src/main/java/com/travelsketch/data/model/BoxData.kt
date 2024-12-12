package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.util.UUID

@IgnoreExtraProperties
data class BoxData(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = UUID.randomUUID().toString(),
    @get:PropertyName("box_x") @set:PropertyName("box_x") var boxX: Int = 0,
    @get:PropertyName("box_y") @set:PropertyName("box_y") var boxY: Int = 0,
    @get:PropertyName("box_z") @set:PropertyName("box_z") var boxZ: Int = 0,
    @get:PropertyName("data") @set:PropertyName("data") var data: String = "",
    @get:PropertyName("degree") @set:PropertyName("degree") var degree: Int = 0,
    @get:PropertyName("height") @set:PropertyName("height") var height: Int? = 0,
    @get:PropertyName("latitude") @set:PropertyName("latitude") var latitude: Double? = 0.0,
    @get:PropertyName("longitude") @set:PropertyName("longitude") var longitude: Double? = 0.0,
    @get:PropertyName("time") @set:PropertyName("time") var time: Long? = 0L,
    @get:PropertyName("width") @set:PropertyName("width") var width: Int? = 0,
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "TEXT"
) {
    fun center(): Pair<Float, Float> {
        return Pair((this.boxX + (this.width ?: 0) / 2f), (this.boxY - (this.height ?: 0) / 2f))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoxData) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Box(id=$id, boxX=$boxX, boxY=$boxY, boxZ=$boxZ, data='$data', type='$type')"
    }
}
