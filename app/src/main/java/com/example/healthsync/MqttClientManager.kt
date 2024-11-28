package com.example.healthsync

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient


class MqttClientManager(private val context: Context) {

    private lateinit var mqttClient: Mqtt5AsyncClient

    fun connectToBroker(
        brokerUrl: String,
        clientId: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            mqttClient = MqttClient.builder()
                .useMqttVersion5()  // Configura MQTT versión 5
                .identifier(clientId)
                .serverHost(brokerUrl.split("//")[1].split(":")[0])
                .serverPort(brokerUrl.split(":").last().toInt())
                .sslWithDefaultConfig()  // Configuración SSL por defecto
                .buildAsync()

            mqttClient.connectWith()
                .simpleAuth()
                .username(username)
                .password(password.toByteArray())
                .applySimpleAuth()
                .send()
                .whenComplete { _, exception ->
                    if (exception == null) {
                        onSuccess() // Éxito en la conexión
                        Log.i("MQTT", "Conexión exitosa al broker")
                    } else {
                        onFailure("Error de conexión: ${exception.message}")
                        Log.e("MQTT", "Error de conexión", exception)
                    }
                }
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
        try {
            mqttClient.publishWith()
                .topic(topic)
                .payload(data.toByteArray())
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete { _, exception ->
                    if (exception == null) {
                        onSuccess()  // Publicación exitosa
                        Log.i("MQTT", "Datos publicados en el tópico: $topic")
                    } else {
                        onFailure("Error al publicar datos: ${exception.message}")
                        Log.e("MQTT", "Error al publicar datos", exception)
                    }
                }
        } catch (e: Exception) {
            Log.e("MQTT", "Error general al publicar datos: ${e.message}", e)
            onFailure("Error general al publicar datos: ${e.message}")
        }
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