package com.example.healthsync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.MqttClient


class MainActivity : AppCompatActivity() {

    private val gson = Gson() // Instancia de Gson para la conversión a JSON
    private lateinit var mqttClientManager: MqttClientManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular el botón con su ID
        val openFilePickerButton: Button = findViewById(R.id.btn_open_file_picker)

        openFilePickerButton.setOnClickListener { // Click en el boton para abrir el explorador de archivos
            openFilePicker()
        }
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
        // Crear una instancia de SQLiteReader
        val sqliteReader = SQLiteReader(this)

        // Abrir la base de datos usando el URI seleccionado
        val database = sqliteReader.openDatabase(uri)

        // Leer la tabla deseada, por ejemplo "activity_data"
        if (database != null) {
            try {
                val data = sqliteReader.readTable(database, "activity_data") // Cambia "activity_data" por el nombre de tu tabla
                val jsonData = convertToJSON(data) //Convierte los datos a json
/*
                // Encripta los datos JSON
                val dataEncryptor = DataEncryptor()
                val secretKey = dataEncryptor.generateKey()
                val encryptedData = dataEncryptor.encrypt(jsonData, secretKey)
 */

                Toast.makeText(this, "Datos leídos: $data", Toast.LENGTH_SHORT).show()
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

    private fun connectToBroker() {
        val brokerUrl = "tcp://localhost:1883"  // Usa la URL de tu broker EMQX
        val clientId = MqttClient.generateClientId()
        val username = "admin"  // Usuario por defecto de EMQX
        val password = "public"  // Contraseña por defecto de EMQX

        mqttClientManager.connectToBroker(brokerUrl, clientId, username, password,
            onSuccess = {
                Toast.makeText(this, "Conectado al broker", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

