@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verifies [signature] of any format — CAdES (`.p7s`, `.sig`), XAdES (`.xml`), ASiC-S, ASiC-E — and reports every
 * check. An invalid signature is a report with [VerificationSummary.isValid] false, not an exception.
 *
 * @param data the data of a detached signature under the names its references use; one file stands for any name
 * @param trustAnchors trusted roots (DER, PEM or a bundle each); a path must end at one of them
 * @param certificates further certificates for the paths (CAs, time-stamping and OCSP services)
 * @param isOnline ask the OCSP responders the certificates name and download missing CA certificates by caIssuers
 *
 * Runs on [Dispatchers.IO]. Throws an [UnreadableInputException] when [signature] is of no format the library knows.
 */
public suspend fun verify(
    signature: ByteArray,
    data: List<NamedBytes> = emptyList(),
    trustAnchors: List<ByteArray> = emptyList(),
    certificates: List<ByteArray> = emptyList(),
    isOnline: Boolean = false,
): VerificationSummary = withContext(Dispatchers.IO) {
    val report = NativeLibrary.checked().verify(
        signature = signature,
        dataNames = data.map { nativeTextOf(it.name, "A file name") }.toTypedArray(),
        dataContents = data.map(NamedBytes::content).toTypedArray(),
        trustAnchors = trustAnchors.toTypedArray(),
        certificates = certificates.toTypedArray(),
        isOnline = isOnline,
    )
    readVerificationSummary(report)
}
