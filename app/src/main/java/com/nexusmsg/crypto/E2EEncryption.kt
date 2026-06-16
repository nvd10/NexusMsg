package com.nexusmsg.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * End-to-End Encryption using ECDH (Curve25519) + AES-256-GCM.
 *
 * Key Exchange:
 * 1. Each user generates a Curve25519 key pair (identity key).
 * 2. Public keys are exchanged via the server (relay).
 * 3. Shared secret = ECDH(own_private_key, peer_public_key).
 * 4. Derived symmetric key = HKDF(shared_secret, salt, info).
 *
 * Message Encryption:
 * - AES-256-GCM with random 12-byte IV/nonce.
 * - Ciphertext = IV || encrypted_data || auth_tag.
 * - Nonce is sent alongside ciphertext.
 *
 * The server NEVER sees the symmetric key — it only relays ciphertext.
 */
@Singleton
class E2EEncryption @Inject constructor(
    private val context: Context
) {
    private val curve25519 = Curve25519.getInstance(Curve25519.BEST)
    private val secureRandom = SecureRandom()

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "nexusmsg_crypto_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── Key Generation ───

    fun generateIdentityKeyPair(): Pair<ByteArray, ByteArray> {
        val keyPair: Curve25519KeyPair = curve25519.generateKeyPair()
        return Pair(keyPair.privateKey, keyPair.publicKey)
    }

    fun getStoredPrivateKey(userId: String): ByteArray? {
        return prefs.getString("private_key_$userId", null)?.let {
            Base64.decode(it, Base64.NO_WRAP)
        }
    }

    fun storeIdentityKey(userId: String, privateKey: ByteArray, publicKey: ByteArray) {
        prefs.edit()
            .putString("private_key_$userId", Base64.encodeToString(privateKey, Base64.NO_WRAP))
            .putString("public_key_$userId", Base64.encodeToString(publicKey, Base64.NO_WRAP))
            .apply()
    }

    fun getStoredPublicKey(userId: String): ByteArray? {
        return prefs.getString("public_key_$userId", null)?.let {
            Base64.decode(it, Base64.NO_WRAP)
        }
    }

    // ─── Shared Secret Computation ───

    fun computeSharedSecret(
        ourPrivateKey: ByteArray,
        theirPublicKey: ByteArray
    ): ByteArray {
        return curve25519.calculateAgreement(theirPublicKey, ourPrivateKey)
    }

    // ─── Key Derivation (HKDF-like) ───

    fun deriveSymmetricKey(sharedSecret: ByteArray, salt: ByteArray = ByteArray(32)): ByteArray {
        // HKDF using HMAC-SHA256
        val prk = hmacSHA256(salt, sharedSecret)
        val info = "nexusmsg-e2e-v1".toByteArray(StandardCharsets.UTF_8)
        return hmacSha256(prk, info + byteArrayOf(0x01)).copyOf(32) // 256-bit AES key
    }

    fun deriveSymmetricKeyForUser(
        ourUserId: String,
        theirUserId: String,
        theirPublicKey: ByteArray
    ): ByteArray {
        val ourPrivateKey = getStoredPrivateKey(ourUserId)
            ?: throw IllegalStateException("Private key not found for $ourUserId")
        val sharedSecret = computeSharedSecret(ourPrivateKey, theirPublicKey)
        val salt = deriveSalt(ourUserId, theirUserId)
        return deriveSymmetricKey(sharedSecret, salt)
    }

    private fun deriveSalt(userId1: String, userId2: String): ByteArray {
        val users = listOf(userId1, userId2).sorted()
        return MessageDigest.getInstance("SHA-256")
            .digest(users.joinToString(":").toByteArray(StandardCharsets.UTF_8))
    }

    // ─── AES-256-GCM Encryption ───

    fun encrypt(message: ByteArray, symmetricKey: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(12)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(symmetricKey, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(message)
        return Pair(ciphertext, nonce)
    }

    fun decrypt(ciphertext: ByteArray, nonce: ByteArray, symmetricKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(symmetricKey, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    // ─── Encrypt/Decrypt message for a specific peer ───

    fun encryptMessage(
        plaintext: String,
        ourUserId: String,
        theirUserId: String,
        theirPublicKey: ByteArray
    ): Pair<String, String> { // returns (ciphertextBase64, nonceBase64)
        val symmetricKey = deriveSymmetricKeyForUser(ourUserId, theirUserId, theirPublicKey)
        val (ciphertext, nonce) = encrypt(
            plaintext.toByteArray(StandardCharsets.UTF_8),
            symmetricKey
        )
        return Pair(
            Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            Base64.encodeToString(nonce, Base64.NO_WRAP)
        )
    }

    fun decryptMessage(
        ciphertextBase64: String,
        nonceBase64: String,
        ourUserId: String,
        theirUserId: String,
        theirPublicKey: ByteArray
    ): String {
        val symmetricKey = deriveSymmetricKeyForUser(ourUserId, theirUserId, theirPublicKey)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val nonce = Base64.decode(nonceBase64, Base64.NO_WRAP)
        val plaintext = decrypt(ciphertext, nonce, symmetricKey)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    // ─── HMAC ───

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun encodeKey(key: ByteArray): String = Base64.encodeToString(key, Base64.NO_WRAP)

    fun decodeKey(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)
}
