package com.manspower.jumpmeasure.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manspower.jumpmeasure.data.local.dao.JumpDao
import com.manspower.jumpmeasure.data.local.entity.JumpEntity

@Database(entities = [JumpEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jumpDao(): JumpDao
}

