package com.example.jumpmeasure.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jumpmeasure.data.local.dao.JumpDao
import com.example.jumpmeasure.data.local.entity.JumpEntity

@Database(entities = [JumpEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jumpDao(): JumpDao
}

