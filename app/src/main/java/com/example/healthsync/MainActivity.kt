package com.example.healthsync

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
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

        // Inicializar mqttClientManager
        mqttClientManager = MqttClientManager(this)

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
            // Intentar abrir la base de datos con el URI seleccionado
            val database = sqliteReader.openDatabase(uri)

            if (database != null) {
                try {
                    // Comprobación de validez de la base de datos
                    val isDatabaseValid = validateDatabase(database)
                    if (!isDatabaseValid) {
                        throw IllegalArgumentException("El archivo no contiene una base de datos SQLite válida.")
                    }

                    // Obtener todas las tablas de la base de datos
                    val tables: List<String>
                    try {
                        tables = sqliteReader.getTables(database)
                        if (tables.isEmpty()) {
                            throw IllegalStateException("La base de datos no contiene tablas.")
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Error al leer las tablas de la base de datos: ${e.message}")
                    }

                    // Mapa para almacenar los datos de todas las tablas
                    val allData = mutableMapOf<String, List<Map<String, Any?>>>()
                    try {
                        for (table in tables) {
                            val tableData = sqliteReader.readTable(database, table)
                            allData[table] = tableData
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Error al procesar los datos de las tablas: ${e.message}")
                    }

                    // Convertir todos los datos a JSON
                    val jsonData: String
                    try {
                        jsonData = convertToJSON(allData)
                    } catch (e: Exception) {
                        throw RuntimeException("Error al convertir los datos a JSON: ${e.message}")
                    }

                    // Conectar al broker y publicar los datos
                    try {
                        connectToBroker(jsonData)
                    } catch (e: Exception) {
                        throw RuntimeException("Error al conectar al broker MQTT: ${e.message}")
                    }

                    // Mostrar mensaje de éxito si sale bien
                    Toast.makeText(this, "Datos leídos y publicados: $jsonData", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    // Manejar errores específicos en el proceso
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                } finally {
                    // Cerrar la base de datos después de usarla
                    database.close()
                }
            } else {
                throw IllegalArgumentException("No se pudo abrir el archivo SQLite.")
            }
        } catch (e: Exception) {
            // Manejo de errores durante la apertura del archivo o conexión
            Toast.makeText(this, "Error general: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Valida si la base de datos contiene al menos una tabla válida.
     */
    private fun validateDatabase(database: SQLiteDatabase): Boolean {
        return try {
            val cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' LIMIT 1", null)
            val hasTables = cursor.moveToFirst()
            cursor.close()
            hasTables
        } catch (e: Exception) {
            false
        }
    }


    // Método para convertir la lista de datos a JSON
    private fun convertToJSON(data: Map<String, List<Map<String, Any?>>>): String {
        return gson.toJson(data) // Utiliza Gson para convertir el mapa a JSON
    }

    private fun connectToBroker(jsonData: String) {
        val brokerUrl = "tcp://172.17.0.1:1883"  // Usa la URL de tu broker EMQX
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

