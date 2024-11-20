package com.example.healthsync

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.io.IOException


class SQLiteReader (private val context: Context){

    fun openDatabase(uri: Uri): SQLiteDatabase? {
        return try {
            val parcelFileDescriptor: ParcelFileDescriptor? =
                context.contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return null

            // Abre la base de datos en modo lectura usando el file descriptor
            SQLiteDatabase.openDatabase(fileDescriptor.toString(), null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Método para obtener todas las tablas de la base de datos
    fun getTables(database: SQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        val cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (cursor.moveToFirst()) {
            do {
                val tableName = cursor.getString(0)
                tables.add(tableName)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return tables
    }


    fun readTable(database: SQLiteDatabase, tableName: String): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        val cursor: Cursor = database.query(tableName, null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val row = mutableMapOf<String, Any?>()
                for (columnIndex in 0 until cursor.columnCount) {
                    val columnName = cursor.getColumnName(columnIndex)
                    row[columnName] = cursor.getString(columnIndex)
                }
                result.add(row)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return result
    }
}
