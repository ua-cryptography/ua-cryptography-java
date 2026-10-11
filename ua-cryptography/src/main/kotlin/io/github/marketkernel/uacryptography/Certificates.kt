@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

/**
 * What [certificate] says; it may be DER, PEM or base64.
 *
 * Throws an [UnreadableInputException] when [certificate] does not parse as an X.509 certificate.
 */
public fun getCertificateDetails(certificate: ByteArray): CertificateDetails =
    readCertificateDetails(NativeLibrary.checked().getCertificateDetails(certificate))
