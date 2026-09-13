package com.classroomassessment.scanner.core.network

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

interface TokenStore {
    fun save(accessToken: String, refreshToken: String?)
    fun accessToken(): String?
    fun refreshToken(): String?
    fun clear()
}

/** Small Android Keystore wrapper; token values never enter logs or plain preferences. */
class EncryptedTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences("secure_session", Context.MODE_PRIVATE)
    private val keyAlias = "classroom_scanner_session_key"

    override fun save(accessToken: String, refreshToken: String?) {
        preferences.edit()
            .putString(ACCESS_TOKEN, encrypt(accessToken))
            .putString(REFRESH_TOKEN, refreshToken?.let(::encrypt))
            .apply()
    }

    override fun accessToken(): String? = preferences.getString(ACCESS_TOKEN, null)?.let(::decrypt)

    override fun refreshToken(): String? = preferences.getString(REFRESH_TOKEN, null)?.let(::decrypt)

    override fun clear() = preferences.edit().clear().apply()

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
        return "$iv:$encrypted"
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2) { "Invalid encrypted session value" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null)
        if (existing is SecretKeySpec) return existing
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
