package com.travelsketch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ViewTypeDao {
    @Query("SELECT * FROM view_type WHERE userId = :userId")
    suspend fun getViewType(userId: String): ViewTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setViewType(viewTypeEntity: ViewTypeEntity)

    @Query("DELETE FROM view_type WHERE userId = :userId")
    suspend fun deleteViewType(userId: String)
}
