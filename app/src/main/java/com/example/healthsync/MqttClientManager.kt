package com.example.healthsync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage


class MqttClientManager(private val context: Context) {

    private lateinit var mqttClient: MqttAndroidClient


    fun connectToBroker(     // Conecta al broker MQTT
        brokerUrl: String,
        clientId: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            mqttClient = MqttAndroidClient(context, brokerUrl, clientId)
            val options = MqttConnectOptions().apply {
                userName = username
                this.password = password.toCharArray()
            }

            mqttClient.connect(options, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    onSuccess()  // Conexión exitosa
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    onFailure("Error de conexión: ${exception?.message}")  // Error de conexión
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Error al inicializar el cliente MQTT: ${e.message}", e)
            onFailure("Error al inicializar el cliente MQTT: ${e.message}")
        }
    }



    fun publishData(
        topic: String,
        data: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Verificar si los datos son un JSON válido
        val isJsonValid = try {
            Gson().fromJson(data, Any::class.java)
            true
        } catch (e: Exception) {
            false
        }

        if (!isJsonValid) {
            onFailure("El contenido proporcionado no es un JSON válido.")
            return
        }

        // Crear el mensaje MQTT
        val message = MqttMessage(data.toByteArray())
        message.qos = 1  // Quality of Service: 1 (entrega al menos una vez)

        mqttClient.publish(topic, message, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                onSuccess()  // Mensaje publicado con éxito
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                onFailure("Error al publicar datos: ${exception?.message}")  // Error al publicar
            }
        })
    }

}


/*    // Recibe los mensajes del broker MQTT
    fun subscribeToTopic(topic: String, onMessageReceived: (String) -> Unit, onFailure: (String) -> Unit) {
        // Establecemos el callback para recibir los mensajes
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                // Cuando se recibe un mensaje, se invoca este callback
                onMessageReceived(message?.toString() ?: "Mensaje vacío")
            }

            override fun connectionLost(cause: Throwable?) {
                // Conexión perdida
                onFailure("Conexión perdida: ${cause?.message}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Confirmación de entrega (no usada en este caso)
            }
        })

        // Suscripción al topic con QoS 1
        mqttClient.subscribe(topic, 1, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                // Suscripción exitosa
                Toast.makeText(context, "Suscripción exitosa al topic: $topic", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                // Error al suscribirse
                onFailure("Error al suscribirse al topic: ${exception?.message}")
            }
        })
    }
}
*/