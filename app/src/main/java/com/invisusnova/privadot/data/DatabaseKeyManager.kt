package com.invisusnova.privadot.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "PrivaDoT_DB_MasterKey"
    private const val PREFS_NAME = "privadot_sec_prefs"
    private const val PREF_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"
    private const val PREF_IV = "db_passphrase_iv"
    private const val AES_GCM_TAG_LENGTH = 128
    private const val PASSPHRASE_BYTE_LENGTH = 32

    @Synchronized
    fun getPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(PREF_IV, null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val secretKey = getOrCreateMasterKey()

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                return cipher.doFinal(encryptedBytes)
            } catch (e: Exception) {
                // If hardware keystore or decryption fails (e.g. device restore), generate fresh key
                e.printStackTrace()
            }
        }

        // Generate fresh 256-bit passphrase
        val rawPassphrase = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(rawPassphrase)

        try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawPassphrase)

            prefs.edit()
                .putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for devices with broken Keystore
            prefs.edit()
                .putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(rawPassphrase, Base64.NO_WRAP))
                .putString(PREF_IV, "FALLBACK_DIRECT")
                .apply()
        }

        return rawPassphrase
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
