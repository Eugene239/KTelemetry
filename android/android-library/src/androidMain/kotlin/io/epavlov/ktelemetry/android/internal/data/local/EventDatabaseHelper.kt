package io.epavlov.ktelemetry.android.internal.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.epavlov.ktelemetry.android.internal.domain.PendingEvent

internal class EventDatabaseHelper(
    context: Context,
) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_JSON TEXT NOT NULL,
                $COL_CREATED INTEGER NOT NULL
            )""",
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        old: Int,
        new: Int,
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insertEvent(json: String) {
        writableDatabase.insert(
            TABLE,
            null,
            ContentValues().apply {
                put(COL_JSON, json)
                put(COL_CREATED, System.currentTimeMillis())
            },
        )
    }

    fun getPendingEvents(limit: Int): List<PendingEvent> {
        val list = mutableListOf<PendingEvent>()
        readableDatabase.query(
            TABLE,
            arrayOf(COL_ID, COL_JSON),
            null,
            null,
            null,
            null,
            "$COL_CREATED ASC",
            "$limit",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list +=
                    PendingEvent(
                        id = cursor.getLong(0),
                        json = cursor.getString(1),
                    )
            }
        }
        return list
    }

    fun getEventCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        val args = ids.map { it.toString() }.toTypedArray()
        writableDatabase.delete(TABLE, "$COL_ID IN ($placeholders)", args)
    }

    companion object {
        private const val DB_NAME = "ktelemetry.db"
        private const val DB_VERSION = 1
        private const val TABLE = "events"
        private const val COL_ID = "id"
        private const val COL_JSON = "event_json"
        private const val COL_CREATED = "created_at"
    }
}
