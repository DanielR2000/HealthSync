package com.example.healthsync

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private val gson = Gson() // Instancia de Gson para la conversión a JSON

    // Registramos el lanzador para abrir documentos (reemplaza a onActivityResult)
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            handleFileUri(it) // Procesa el archivo seleccionado
        } ?: run {
            Toast.makeText(this, "No se seleccionó ningún archivo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular el botón con su ID
        val openFilePickerButton: Button = findViewById(R.id.btn_open_file_picker)

        openFilePickerButton.setOnClickListener {
            openFilePicker() // Abre el selector de archivos
        }
    }

    // Método para abrir el selector de archivos (SAF)
    private fun openFilePicker() {
        openDocumentLauncher.launch(arrayOf("*/*")) // Permite seleccionar cualquier tipo de archivo
    }

    // Método para manejar el archivo seleccionado
    private fun handleFileUri(uri: Uri) {
        // Crear una instancia de SQLiteReader
        val sqliteReader = SQLiteReader(this)

        // Abrir la base de datos usando el URI seleccionado
        val database = sqliteReader.openDatabase(uri)

        // Leer la tabla deseada, por ejemplo "activity_data"
        if (database != null) {
            try {
                val data = sqliteReader.readTable(database, "activity_data") // Cambia "activity_data" por el nombre de tu tabla
                val jsonData = convertToJSON(data) // Convierte los datos a JSON

                // Encripta los datos JSON
                val dataEncryptor = DataEncryptor()
                val secretKey = dataEncryptor.generateKey()
                val encryptedData = dataEncryptor.encrypt(jsonData, secretKey)

                Toast.makeText(this, "Datos leídos: $data", Toast.LENGTH_SHORT).show()

                // Aquí podrías publicar los datos cifrados a través de MQTT o guardarlos en otro lugar

            } catch (e: Exception) {
                Toast.makeText(this, "Error al leer la tabla: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                database.close() // Asegúrate de cerrar la base de datos
            }
        } else {
            Toast.makeText(this, "No se pudo abrir el archivo SQLite", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para convertir la lista de datos a JSON
    private fun convertToJSON(data: List<Map<String, Any?>>): String {
        return gson.toJson(data) // Utiliza Gson para convertir la lista a JSON
    }
}
