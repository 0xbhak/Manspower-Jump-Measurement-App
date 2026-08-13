package com.example.jumpmeasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jumpmeasure.data.local.entity.JumpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JumpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJump(jump: JumpEntity)

    @Query("SELECT * FROM jumps ORDER BY timestamp DESC")
    fun getAllJumps(): Flow<List<JumpEntity>>

    @Query("DELETE FROM jumps")
    suspend fun deleteAllJumps()
}
