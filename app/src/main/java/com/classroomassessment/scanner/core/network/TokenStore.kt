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
    fun save(accessToken: String, refreshToken: String?, account: Account? = null)
    fun saveIdentity(account: Account)
    fun accessToken(): String?
    fun refreshToken(): String?
    fun account(): Account?
    fun clear()
}

/** Small Android Keystore wrapper; token values never enter logs or plain preferences. */
class EncryptedTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences("secure_session", Context.MODE_PRIVATE)
    private val keyAlias = "classroom_scanner_session_key"

    override fun save(accessToken: String, refreshToken: String?, account: Account?) {
        val editor = preferences.edit()
            .putString(ACCESS_TOKEN, encrypt(accessToken))
            .putString(REFRESH_TOKEN, refreshToken?.let(::encrypt))
        if (account != null) writeIdentity(editor, account)
        editor.apply()
    }

    override fun saveIdentity(account: Account) {
        writeIdentity(preferences.edit(), account).apply()
    }

    override fun accessToken(): String? = preferences.getString(ACCESS_TOKEN, null)?.let(::decrypt)

    override fun refreshToken(): String? = preferences.getString(REFRESH_TOKEN, null)?.let(::decrypt)

    override fun account(): Account? = runCatching {
        Account(
            id = decryptRequired(USER_ID),
            displayName = decryptRequired(DISPLAY_NAME),
            email = decryptRequired(EMAIL),
            organizationId = decryptRequired(ORGANIZATION_ID),
            organizationName = decryptRequired(ORGANIZATION_NAME),
            teacherId = decryptRequired(TEACHER_ID),
            teacherName = decryptRequired(TEACHER_NAME),
            loginName = decryptRequired(LOGIN_NAME),
            role = decryptRequired(ROLE),
        )
    }.getOrNull()

    override fun clear() = preferences.edit().clear().apply()

    private fun writeIdentity(editor: android.content.SharedPreferences.Editor, account: Account): android.content.SharedPreferences.Editor = editor
        .putString(USER_ID, encrypt(account.id))
        .putString(DISPLAY_NAME, encrypt(account.displayName))
        .putString(EMAIL, encrypt(account.email))
        .putString(ORGANIZATION_ID, encrypt(account.organizationId))
        .putString(ORGANIZATION_NAME, encrypt(account.organizationName))
        .putString(TEACHER_ID, encrypt(account.teacherId))
        .putString(TEACHER_NAME, encrypt(account.teacherName))
        .putString(LOGIN_NAME, encrypt(account.loginName))
        .putString(ROLE, encrypt(account.role))

    private fun decryptRequired(key: String): String = preferences.getString(key, null)?.let(::decrypt)
        ?: error("Missing session identity")

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
        const val USER_ID = "user_id"
        const val DISPLAY_NAME = "display_name"
        const val EMAIL = "email"
        const val ORGANIZATION_ID = "organization_id"
        const val ORGANIZATION_NAME = "organization_name"
        const val TEACHER_ID = "teacher_id"
        const val TEACHER_NAME = "teacher_name"
        const val LOGIN_NAME = "login_name"
        const val ROLE = "role"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
