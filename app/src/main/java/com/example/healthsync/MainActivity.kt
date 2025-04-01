package com.example.healthsync

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.Gson
import org.json.JSONObject
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties
import java.util.UUID
//import com.hivemq.client.mqtt.mqtt5.datatypes.MqttUserProperty
//import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5UserProperties
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import javax.crypto.Cipher
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.spec.GCMParameterSpec


class MainActivity : AppCompatActivity() {

    private val gson = Gson() // Instancia de Gson para la conversión a JSON
    private lateinit var mqttClientManager: MqttClientManager
    // Variables globales para almacenar los valores de _id e IDENTIFIER
    private var deviceId: String? = null
    private var deviceIdentifier: String? = null

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

        val showDeviceInfoButton: Button = findViewById(R.id.btn_show_device_info)

        showDeviceInfoButton.setOnClickListener {
            showDeviceInfo()
        }
    }

    private fun showDeviceInfo() {
        val message = "Device USER ID: ${deviceId ?: "No encontrado"}\nDevice Identifier MAC: ${deviceIdentifier ?: "No encontrado"}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


    // Método para abrir el selector de archivos (SAF)
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type =
                "*/*" // Permite seleccionar cualquier tipo de archivo; puedes ajustar esto a "application/x-sqlite3" si aplica
        }
        filePickerLauncher.launch(intent)
    }


    // Registramos el lanzador de resultados de la actividad para manejar el archivo seleccionado
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? =
                    result.data?.data //Esta línea obtiene el Uri del archivo que el usuario ha seleccionado
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
            val database = sqliteReader.openDatabase(uri) ?: run {
                Toast.makeText(this, "No se pudo abrir el archivo SQLite.", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            try {
                val isDatabaseValid = validateDatabase(database)
                if (!isDatabaseValid) {
                    Toast.makeText(
                        this,
                        "El archivo no contiene una base de datos SQLite válida.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error validando la base de datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR", "Error validando la base de datos", e)
                return
            }

            val tables: List<String>
            try {
                tables = sqliteReader.getTables(database)
                if (tables.isEmpty()) {
                    Toast.makeText(this, "La base de datos no contiene tablas.", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al leer las tablas de la base de datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR", "Error al leer las tablas de la base de datos", e)
                return
            }

            val allData = mutableMapOf<String, List<Map<String, Any?>>>()
            try {
                for (table in tables) {
                    val tableData = sqliteReader.readTable(database, table)
                    allData[table] = tableData
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al procesar los datos de las tablas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR", "Error al procesar los datos de las tablas", e)
                return
            }

            val jsonData: String
            try {
                jsonData = convertToJSON(allData)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al convertir los datos a JSON: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR", "Error al convertir los datos a JSON", e)
                return
            }

            // Extraer los valores de _id y IDENTIFIER de la tabla DEVICE
            try {
                val jsonObject = JSONObject(jsonData)
                if (jsonObject.has("DEVICE")) {
                    val deviceArray = jsonObject.getJSONArray("DEVICE")
                    if (deviceArray.length() > 0) {
                        val firstDevice = deviceArray.getJSONObject(0)
                        deviceId = firstDevice.optString("_id")
                        deviceIdentifier = firstDevice.optString("IDENTIFIER")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al extraer datos de DEVICE: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ERROR", "Error al extraer datos de DEVICE", e)
            }
            try {
                connectToBroker2(jsonData)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al conectar al broker MQTT: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR", "Error al conectar al broker MQTT", e)
                return
            }

            Toast.makeText(this, "Datos leídos y publicados: $jsonData", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error general: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ERROR", "Error general en handleFileUri", e)
        }
    }


    /**
     * Valida si la base de datos contiene al menos una tabla válida.
     */
    private fun validateDatabase(database: SQLiteDatabase): Boolean {
        return try {
            val cursor =
                database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' LIMIT 1", null)
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

    private fun fragmentAndPublishData(jsonData: String, topic: String, aesKey: String) {
        val chunkSize = 512 * 1024 // Tamaño de cada fragmento (512 KB)
        val fragments = jsonData.chunked(chunkSize) // Divide el JSON en fragmentos

        for ((index, fragment) in fragments.withIndex()) {
            // Cifrar el fragmento antes de enviarlo
            //val encryptedFragment = encryptWithAES(fragment, aesKey)

            val fragmentPayload = JSONObject().apply {
                put("message_id", "ID_1" )
                put("index", index)
                put("total", fragments.size)
                put("data", fragment)
                //put("data", encryptedFragment) // Enviar el fragmento cifrado
            }.toString()

            mqttClientManager.publishData(
                topic, fragmentPayload,
                onSuccess = {
                    Log.i("MQTT", "Fragmento $index/${fragments.size - 1} enviado")
                },
                onFailure = { errorMessage ->
                    Log.e("MQTT", "Error al enviar fragmento $index: $errorMessage")
                }
            )
        }
    }





    /*
        private fun fragmentAndPublishData(jsonData: String, topic: String) {
            val chunkSize = 512 * 1024 // Tamaño de cada fragmento (512 KB)
            val fragments = jsonData.chunked(chunkSize) // Divide el JSON en fragmentos
            val identifier = UUID.randomUUID().toString() // Identificador único del mensaje

            for ((index, fragment) in fragments.withIndex()) {
                // Construcción de User Properties con el nuevo método
                val userProperties = Mqtt5UserProperties.builder()
                    .add("fragment.identifier", identifier)
                    .add("fragment.index", index.toString())
                    .add("fragment.count", fragments.size.toString())
                    .build()

                mqttClientManager.publishDataWithProperties(
                    topic,
                    fragment,
                    userProperties,
                    onSuccess = {
                        Log.i("MQTT", "Fragmento $index/${fragments.size - 1} enviado")
                    },
                    onFailure = { errorMessage ->
                        Log.e("MQTT", "Error al enviar fragmento $index: $errorMessage")
                    }
                )
            }
        }
    */

    private fun connectToBroker2(jsonData: String) {
        val brokerUrl = "ssl://uba2933f.ala.eu-central-1.emqxsl.com:8883"
        val clientId = "danielrc7"
        val username = "admin"
        val password = "public"
        val topic = "health/data"

        // Obtener la clave AES desde el servidor
        //val aesKey = getAesKeyFromServer("https://flask-aes-render.onrender.com/get-key") ?: return
        val aesKey = "0ffAag5jFesLTj9S3B_fmE6WNWuj-EpjtcJd68WvDFs"
        mqttClientManager.connectToBroker(
            brokerUrl, clientId, username, password,
            onSuccess = {
                // Fragmentar y publicar los datos cifrados
                fragmentAndPublishData(jsonData, topic, aesKey)
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Error al conectar: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }



    fun getAesKeyFromServer(url: String): String? {
        var aesKey: String? = null
        try {
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStreamReader = InputStreamReader(connection.inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val response = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                response.append(line)
            }

            // Parsear la respuesta JSON y obtener la clave AES
            val jsonResponse = JSONObject(response.toString())
            aesKey = jsonResponse.getString("aes_key") // Extraer el valor de "aes_key"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return aesKey
    }





    fun encryptWithAES(data: String, aesKey: String): String {
        return try {
            val fixedAesKey = aesKey.replace('-', '+').replace('_', '/')
            val keyBytes = Base64.decode(fixedAesKey, Base64.DEFAULT)
            val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedData = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encryptedData, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR: ${e.message}"
        }
    }







}







