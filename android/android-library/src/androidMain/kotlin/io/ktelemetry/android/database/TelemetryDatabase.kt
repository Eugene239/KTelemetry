package io.ktelemetry.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TelemetryEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryEventDao(): TelemetryEventDao

    companion object {
        @Volatile
        private var INSTANCE: TelemetryDatabase? = null

        fun getDatabase(context: Context): TelemetryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TelemetryDatabase::class.java,
                    "k_telemetry_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

