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

        openFilePickerButton.setOnClickListener { // Click en el boton para abrir el explorador de archivos y procesa el archivo sellecionado
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
        val sqliteReader = SQLiteReader(this)

        try {
            // Abrir la base de datos usando el URI seleccionado
            val database = sqliteReader.openDatabase(uri)

            if (database != null) {
                try {
                    // Obtener todas las tablas de la base de datos
                    val tables = sqliteReader.getTables(database)

                    // Mapa para almacenar los datos de todas las tablas
                    val allData = mutableMapOf<String, List<Map<String, Any?>>>()

                    // Leer los datos de todas las tablas y almacenarlos en el mapa
                    for (table in tables) {
                        val tableData = sqliteReader.readTable(database, table)
                        allData[table] = tableData
                    }

                    // Convertir todos los datos a JSON
                    val jsonData = convertToJSON(allData)

                    // Conectar al broker y publicar los datos
                    connectToBroker(jsonData)

                    Toast.makeText(this, "Datos leídos y publicados: $jsonData", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al leer las tablas: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    // Asegúrate de cerrar la base de datos después de la operación
                    database.close()
                }
            } else {
                Toast.makeText(this, "No se pudo abrir el archivo SQLite", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    // Método para convertir la lista de datos a JSON
    private fun convertToJSON(data: Map<String, List<Map<String, Any?>>>): String {
        return gson.toJson(data) // Utiliza Gson para convertir el mapa a JSON
    }

    private fun connectToBroker(jsonData: String) {
        val brokerUrl = "tcp://172.17.255.255:1883"  // Usa la URL de tu broker EMQX
        val clientId = MqttClient.generateClientId()
        val username = "admin"  // Usuario por defecto de EMQX
        val password = "public"  // Contraseña por defecto de EMQX

        mqttClientManager.connectToBroker(brokerUrl, clientId, username, password,
            onSuccess = {
                // Publica los datos una vez conectado
                mqttClientManager.publishData("health/data", jsonData,
                    onSuccess = {
                        Toast.makeText(this, "Datos publicados exitosamente", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, "Error al publicar datos: $errorMessage", Toast.LENGTH_SHORT).show()
                    })
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Error de conexión: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }


//No se llama a este método
    private fun publishData(topic: String, jsonData: String) {
        mqttClientManager.publishData(topic, jsonData,
            onSuccess = {
                Toast.makeText(this, "Datos publicados exitosamente", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Error al publicar datos: $errorMessage", Toast.LENGTH_SHORT).show()
            })
    }
}

