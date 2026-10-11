@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Signs [files] with the key [alias] of [keyStore] (without [alias], its one signing key), as [options] say, and
 * returns the signature: the `.p7s` / `.sig`, the `.xml`, or the `.asics` / `.asice` container.
 *
 * CAdES and ASiC-S sign one file. [certificates] complete the signer's chain (DER, PEM or a bundle each). Runs on
 * [Dispatchers.IO]: levels T and LT ask the time-stamping service, OCSP responders and caIssuers over the network.
 *
 * Throws a [WrongPasswordException] when the store does not open with [password], an [UnreadableInputException] when
 * it does not parse, an [InvalidArgumentException] for options that do not fit the format or the key, and a
 * [ServiceFailureException] when a service of level T or LT does not answer.
 */
public suspend fun sign(
    keyStore: ByteArray,
    password: String,
    files: List<NamedBytes>,
    alias: String? = null,
    options: SigningOptions = SigningOptions(),
    certificates: List<ByteArray> = emptyList(),
): ByteArray = withContext(Dispatchers.IO) {
    withNativePassword(password) { nativePassword ->
        NativeLibrary.checked().sign(
            keyStore = keyStore,
            password = nativePassword,
            alias = alias?.let { nativeTextOf(it, "The alias") },
            fileNames = files.map { nativeTextOf(it.name, "A file name") }.toTypedArray(),
            fileContents = files.map(NamedBytes::content).toTypedArray(),
            certificates = certificates.toTypedArray(),
            format = nativeCodeOf(options.format),
            level = nativeCodeOf(options.level),
            hash = nativeCodeOf(options.hash),
            isDetached = options.isDetached,
            hasContentTimeStamp = options.hasContentTimeStamp,
            tsaUrl = options.tsaUrl?.let { nativeTextOf(it, "The time-stamping service URL") },
            ocspUrl = options.ocspUrl?.let { nativeTextOf(it, "The OCSP responder URL") },
        )
    }
}
