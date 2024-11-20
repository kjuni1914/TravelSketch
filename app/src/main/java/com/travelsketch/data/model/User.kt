package com.travelsketch.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

// Ignore fields if there are not defined
@IgnoreExtraProperties
data class User(
    // Mapping field name to constant name
    @PropertyName("canvas_ids") val canvasIds: String? = null,
    @PropertyName("friends_ids") val friendsIds: String? = null,
    @PropertyName("phone_number") val phoneNumber: String? = null,
    @PropertyName("view_type") val viewType: Boolean? = null
)
