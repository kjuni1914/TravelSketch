package com.travelsketch.data.local

import androidx.room.TypeConverter
import com.travelsketch.data.model.ViewType

class ViewTypeConverter {
    @TypeConverter
    fun fromViewType(viewType: ViewType): String {
        return viewType.name
    }

    @TypeConverter
    fun toViewType(value: String): ViewType {
        return ViewType.valueOf(value)
    }
}