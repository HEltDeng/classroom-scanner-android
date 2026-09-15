package com.classroomassessment.scanner.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScanEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        fun create(context: Context): ScannerDatabase = Room.databaseBuilder(
            context,
            ScannerDatabase::class.java,
            "classroom_scanner.db",
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_queue ADD COLUMN scan_session_id TEXT")
            }
        }
    }
}
