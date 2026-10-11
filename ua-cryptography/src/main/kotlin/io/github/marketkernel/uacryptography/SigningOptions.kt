package io.github.marketkernel.uacryptography

/** How [sign] signs; the defaults are those of the national PKI: CAdES-B, enveloped, DSTU 7564-256. */
public data class SigningOptions(
    /** The format; CAdES and ASiC-S sign one file. */
    val format: SignatureFormat = SignatureFormat.CADES,
    /** B offline; T asks the time-stamping service, LT also the OCSP responders and caIssuers. */
    val level: SignatureLevel = SignatureLevel.B,
    /** The hash of the signature. */
    val hash: SignatureHash = SignatureHash.DSTU7564,
    /** CAdES without the data inside. */
    val isDetached: Boolean = false,
    /** CAdES and ASiC-S: also time-stamp the data itself (asks the time-stamping service). */
    val hasContentTimeStamp: Boolean = false,
    /** The time-stamping service; null: the one the signer's certificate names. */
    val tsaUrl: String? = null,
    /** The OCSP responder for every certificate; null: each certificate's own. */
    val ocspUrl: String? = null,
)
