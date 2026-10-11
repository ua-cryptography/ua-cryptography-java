package io.github.marketkernel.uacryptography.example.trust

import java.util.Base64

/**
 * The DER certificates in a certificate file: DER, PEM (one or several) or a PKCS#7 bundle (`.p7b`, DER or PEM).
 *
 * Throws an [IllegalArgumentException] when the file holds no certificate.
 */
fun splitCertificates(bytes: ByteArray): List<ByteArray> {
    val pemBlocks = readPemBlocks(bytes)
    val ders = pemBlocks.ifEmpty { listOf(bytes) }
    val certificates = ders.flatMap { der -> if (isPkcs7(der)) readPkcs7Certificates(der) else listOf(der) }
    require(certificates.isNotEmpty()) { "The file holds no certificate" }
    return certificates
}

private val PEM_BLOCK = Regex("-----BEGIN ([A-Z0-9 ]+)-----([\\s\\S]*?)-----END \\1-----")
private val WHITESPACE = Regex("\\s")

private fun readPemBlocks(bytes: ByteArray): List<ByteArray> {
    val text = String(bytes, Charsets.ISO_8859_1)
    return PEM_BLOCK.findAll(text).map { Base64.getDecoder().decode(it.groupValues[2].replace(WHITESPACE, "")) }.toList()
}

// ContentInfo ::= SEQUENCE { contentType OID 1.2.840.113549.1.7.2 (signedData), content [0] EXPLICIT SignedData }
private val SIGNED_DATA_OID = byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x02)
private const val SEQUENCE_TAG = 0x30
private const val CONTEXT_ZERO_TAG = 0xA0

// version, digestAlgorithms and encapContentInfo come before the certificates of a SignedData.
private const val ELEMENTS_BEFORE_CERTIFICATES = 3

private fun isPkcs7(der: ByteArray): Boolean {
    if (der.isEmpty() || der[0].toInt() and 0xFF != SEQUENCE_TAG) {
        return false
    }
    val contentStart = readDerElement(der, 0).contentStart
    if (contentStart + SIGNED_DATA_OID.size > der.size) {
        return false
    }
    return SIGNED_DATA_OID.indices.all { index -> der[contentStart + index] == SIGNED_DATA_OID[index] }
}

// SignedData ::= SEQUENCE { version, digestAlgorithms SET, encapContentInfo SEQUENCE,
//   certificates [0] IMPLICIT SET OF Certificate OPTIONAL, ... }
private fun readPkcs7Certificates(der: ByteArray): List<ByteArray> {
    val contentInfo = readDerElement(der, 0)
    val contentType = readDerElement(der, contentInfo.contentStart)
    val explicitContent = readDerElement(der, contentType.end)
    val signedData = readDerElement(der, explicitContent.contentStart)
    var offset = signedData.contentStart
    repeat(ELEMENTS_BEFORE_CERTIFICATES) { offset = readDerElement(der, offset).end }
    if (offset >= signedData.end || der[offset].toInt() and 0xFF != CONTEXT_ZERO_TAG) {
        return emptyList()
    }
    val certificateSet = readDerElement(der, offset)
    val certificates = mutableListOf<ByteArray>()
    var position = certificateSet.contentStart
    while (position < certificateSet.end) {
        val certificate = readDerElement(der, position)
        certificates += der.copyOfRange(position, certificate.end)
        position = certificate.end
    }
    return certificates
}

/** One tag-length-value of DER: where its content starts and where it ends. */
private class DerElement(val contentStart: Int, val end: Int)

private const val LONG_LENGTH_FLAG = 0x80
private const val LENGTH_BYTES_MASK = 0x7F
private const val MAX_LENGTH_BYTES = 4
private const val TAG_AND_LENGTH_BYTES = 2

private fun readDerElement(der: ByteArray, offset: Int): DerElement {
    require(offset + TAG_AND_LENGTH_BYTES <= der.size) { "Truncated DER" }
    val lengthByte = der[offset + 1].toInt() and 0xFF
    var position = offset + TAG_AND_LENGTH_BYTES
    var length = lengthByte
    if (lengthByte and LONG_LENGTH_FLAG != 0) {
        val lengthBytes = lengthByte and LENGTH_BYTES_MASK
        require(lengthBytes in 1..MAX_LENGTH_BYTES && position + lengthBytes <= der.size) { "Unsupported DER length" }
        length = 0
        repeat(lengthBytes) { length = (length shl 8) or (der[position++].toInt() and 0xFF) }
    }
    require(length >= 0 && position + length <= der.size) { "Truncated DER" }
    return DerElement(position, position + length)
}
