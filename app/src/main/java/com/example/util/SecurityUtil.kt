package com.example.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding" // Sederhana namun aman untuk demonstrasi lokal

    /**
     * Enkripsi data penting (NIP, Password, dsb.) menggunakan AES.
     */
    fun encrypt(data: String, key: String = "KKGPAISukajaya26"): String {
        return try {
            val secretKey = generateKey(key)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            data // Fallback jika terjadi kesalahan
        }
    }

    /**
     * Dekripsi data yang terenkripsi.
     */
    fun decrypt(encryptedData: String, key: String = "KKGPAISukajaya26"): String {
        return try {
            val secretKey = generateKey(key)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedData // Fallback jika terjadi kesalahan
        }
    }

    private fun generateKey(key: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = key.toByteArray(Charsets.UTF_8)
        val hash = digest.digest(bytes)
        return SecretKeySpec(hash.copyOf(16), ALGORITHM) // Ambil 128 bit pertama untuk AES
    }

    /**
     * Simulasi validasi keamanan jaringan HTTPS.
     */
    fun verifyHttpsConnection(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    /**
     * Validasi format login NIP & Password/Pin.
     */
    fun validateNip(nip: String): Boolean {
        // NIP biasanya terdiri dari angka, minimal 8 karakter (simulasi sederhana)
        val cleanNip = nip.replace("\\s".toRegex(), "")
        return cleanNip.length >= 8 && cleanNip.all { it.isDigit() }
    }
}
