package com.example.healthsync

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
import java.io.IOException


class SQLiteReader (private val context: Context){

    fun openDatabase(uri: Uri): SQLiteDatabase? {
        return try {
            // Usa un InputStream para copiar el contenido del archivo a un archivo temporal
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("tempdb", ".sqlite", context.cacheDir)
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()

            // Abre la base de datos desde el archivo temporal
            SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
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
