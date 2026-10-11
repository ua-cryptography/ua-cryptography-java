package io.github.marketkernel.uacryptography

/** A key store made by [createKeyStore] and the certificate of its key. */
public class CreatedKeyStore(
    /** The JKS bytes. */
    public val keyStore: ByteArray,
    /** The self-signed certificate, DER. */
    public val certificate: ByteArray,
)
