package io.github.marketkernel.uacryptography

/** The level of a signature. */
public enum class SignatureLevel {
    /** The basic signature, made offline. */
    B,

    /** With a signature time-stamp from the time-stamping service. */
    T,

    /** With the CA certificates and OCSP responses too: XAdES-B-LT, CAdES-X Long. */
    LT,
}
