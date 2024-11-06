package com.example.healthsync

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

class DataEncryptor {

    private val cipher = Cipher.getInstance("AES")

    // Genera una clave AES de 128 bits
    fun generateKey(): Key {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        return keyGen.generateKey()
    }

    // Encripta los datos JSON
    fun encrypt(data: String, secretKey: Key): String {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedData = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    // Desencripta los datos JSON
    fun decrypt(data: String, secretKey: Key): String {
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decodedData = Base64.decode(data, Base64.DEFAULT)
        val decryptedData = cipher.doFinal(decodedData)
        return String(decryptedData)
    }
}
