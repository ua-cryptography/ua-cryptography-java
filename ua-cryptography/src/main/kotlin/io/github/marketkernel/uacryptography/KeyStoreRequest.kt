package io.github.marketkernel.uacryptography

/** A new JKS key store with one DSTU 4145 key on a curve of table Г.1 and its self-signed certificate. */
public data class KeyStoreRequest(
    /** The subject, e.g. `CN=Name,O=Organization,C=UA`. */
    val subject: String,
    /** The alias of the key entry. */
    val alias: String = "key",
    /** The degree m of the curve of table Г.1; 257 is the one of the national PKI. */
    val m: Int = 257,
    /** How many days the certificate is valid. */
    val validityDays: Int = 365,
    /** The hash the certificate is signed over. */
    val signatureHash: SignatureHash = SignatureHash.DSTU7564,
)
