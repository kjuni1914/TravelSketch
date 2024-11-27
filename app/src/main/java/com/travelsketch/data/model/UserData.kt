package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

// Ignore fields if there are not defined
@IgnoreExtraProperties
data class UserData(
    // Mapping field name to constant name
    @get:PropertyName("canvas_ids") @set:PropertyName("canvas_ids") var canvasIds: String = "",
    @get:PropertyName("friends_ids") @set:PropertyName("friends_ids") var friendIds: String = "",
    @get:PropertyName("phone_number") @set:PropertyName("phone_number") var phoneNumber: String = "",
    @get:PropertyName("view_type") @set:PropertyName("view_type") var viewType: Boolean = true
) {
    override fun toString(): String {
        return "User(canvasIds='$canvasIds', friendIds='$friendIds', phoneNumber='$phoneNumber', viewType=$viewType)"
    }
}