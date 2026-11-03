package com.template.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.data.local.entity.SavedAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM taxi_addresses ORDER BY usedAt DESC LIMIT 6")
    fun observeRecent(): Flow<List<SavedAddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(address: SavedAddressEntity)

    @Query("DELETE FROM taxi_addresses WHERE text = :text")
    suspend fun delete(text: String)
}
