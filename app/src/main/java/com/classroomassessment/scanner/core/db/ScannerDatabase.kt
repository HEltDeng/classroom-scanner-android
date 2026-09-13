package com.classroomassessment.scanner.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        fun create(context: Context): ScannerDatabase = Room.databaseBuilder(
            context,
            ScannerDatabase::class.java,
            "classroom_scanner.db",
        ).build()
    }
}
