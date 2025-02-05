package com.example.healthsync

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties


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
                .serverHost(brokerUrl.split(":")[1].replace("//", ""))
                .serverPort(brokerUrl.split(":")[2].toInt())
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
                .qos(MqttQos.AT_LEAST_ONCE) // Puedes cambiar el QoS según necesites
                .send()
                .whenComplete { _, exception ->
                    if (exception == null) {
                        onSuccess()
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

    /*
    fun publishDataWithProperties(
        topic: String,
        data: String,
        userProperties: Mqtt5UserProperties,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            mqttClient.publishWith()
                .topic(topic)
                .payload(data.toByteArray())
                .qos(MqttQos.AT_LEAST_ONCE) // Puedes cambiar el QoS según necesites
                .userProperties(userProperties) // Aquí agregamos las propiedades
                .send()
                .whenComplete { _, exception ->
                    if (exception == null) {
                        onSuccess()
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
    */

}
