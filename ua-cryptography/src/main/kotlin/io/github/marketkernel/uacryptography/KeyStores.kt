@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A new JKS key store protected by [password] (the key too), with a DSTU 4145 key generated from the operating
 * system's random source and its self-signed certificate. Runs on [Dispatchers.IO].
 *
 * Throws an [InvalidArgumentException] for a curve degree m that table Г.1 does not list, or a subject that does not
 * parse.
 */
public suspend fun createKeyStore(request: KeyStoreRequest, password: String): CreatedKeyStore =
    withContext(Dispatchers.IO) {
        val created = withNativePassword(password) { nativePassword ->
            NativeLibrary.checked().createKeyStore(
                subject = nativeTextOf(request.subject, "The subject"),
                alias = nativeTextOf(request.alias, "The alias"),
                m = nativeUnsignedOf(request.m, "The curve degree m"),
                validityDays = nativeUnsignedOf(request.validityDays, "The validity in days"),
                signatureHash = nativeCodeOf(request.signatureHash),
                password = nativePassword,
            )
        }
        CreatedKeyStore(keyStore = created[0], certificate = created[1])
    }

/**
 * The certificate (DER) of the signing key [alias] of [keyStore]; without [alias], of its one signing key.
 *
 * Throws an [UnreadableInputException] when [keyStore] is not a JKS store, a [WrongPasswordException] when it does not
 * open with [password], and an [InvalidArgumentException] when it has no such key, or several and no [alias].
 */
@JvmOverloads
public fun getCertificate(keyStore: ByteArray, password: String, alias: String? = null): ByteArray =
    withNativePassword(password) { nativePassword ->
        NativeLibrary.checked().getCertificate(keyStore, nativePassword, alias?.let { nativeTextOf(it, "The alias") })
    }

/**
 * The entries of [keyStore], opened with [password].
 *
 * Throws an [UnreadableInputException] when [keyStore] is not a JKS store and a [WrongPasswordException] when it does
 * not open with [password].
 */
public fun getKeyStoreEntries(keyStore: ByteArray, password: String): List<KeyStoreEntry> {
    val json = withNativePassword(password) { nativePassword ->
        NativeLibrary.checked().getKeyStoreEntries(keyStore, nativePassword)
    }
    return readKeyStoreEntries(json)
}
