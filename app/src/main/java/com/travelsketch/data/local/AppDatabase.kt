package com.travelsketch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ViewTypeEntity::class], version = 1)
@TypeConverters(ViewTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun viewTypeDao(): ViewTypeDao
}