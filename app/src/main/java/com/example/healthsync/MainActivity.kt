package com.example.healthsync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openFilePicker() // Llama a la función para abrir el explorador de archivos cuando lo necesites
    }

    // Método para abrir el selector de archivos (SAF)
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Permite seleccionar cualquier tipo de archivo; puedes ajustar esto a "application/x-sqlite3" si aplica
        }
        filePickerLauncher.launch(intent)
    }

    // Registramos el lanzador de resultados de la actividad para manejar el archivo seleccionado
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data //Esta línea obtiene el Uri del archivo que el usuario ha seleccionado
            uri?.let {

                handleFileUri(it) // Aquí se maneja el archivo seleccionado
            }
        } else {
            Toast.makeText(this, "No se seleccionó ningún archivo", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para manejar el archivo seleccionado
    private fun handleFileUri(uri: Uri) {
        // Aquí es donde puedes leer el archivo SQLite
        Toast.makeText(this, "Archivo seleccionado: $uri", Toast.LENGTH_SHORT).show()
        // Ejemplo: abrir una conexión de lectura a la base de datos usando el URI
        // Más adelante añadiremos el código para leer datos del archivo SQLite
    }

}

