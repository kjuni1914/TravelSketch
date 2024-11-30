package com.travelsketch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.travelsketch.data.model.ViewType

@Entity(tableName = "view_type")
data class ViewTypeEntity(
    @PrimaryKey val userId: String,
    val viewType: ViewType
)