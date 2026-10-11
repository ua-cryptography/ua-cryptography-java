package io.github.marketkernel.uacryptography

/** The `UaStatus` values of ua_cryptography.h other than OK. */
internal object NativeStatus {
    const val INVALID_ARGUMENT = 1
    const val UNREADABLE_INPUT = 2
    const val WRONG_PASSWORD = 3
    const val AUTHENTICATION_FAILED = 4
    const val SERVICE_FAILURE = 5
    const val INTERNAL = 6
}

/** The C ABI's `UA_SIGNATURE_FORMAT_…` of [format]. */
internal fun nativeCodeOf(format: SignatureFormat): Int = when (format) {
    SignatureFormat.CADES -> UA_SIGNATURE_FORMAT_CADES
    SignatureFormat.XADES -> UA_SIGNATURE_FORMAT_XADES
    SignatureFormat.ASIC_S -> UA_SIGNATURE_FORMAT_ASIC_S
    SignatureFormat.ASIC_E -> UA_SIGNATURE_FORMAT_ASIC_E
}

/** The C ABI's `UA_SIGNATURE_LEVEL_…` of [level]. */
internal fun nativeCodeOf(level: SignatureLevel): Int = when (level) {
    SignatureLevel.B -> UA_SIGNATURE_LEVEL_B
    SignatureLevel.T -> UA_SIGNATURE_LEVEL_T
    SignatureLevel.LT -> UA_SIGNATURE_LEVEL_LT
}

/** The C ABI's `UA_SIGNATURE_HASH_…` of [hash]. */
internal fun nativeCodeOf(hash: SignatureHash): Int = when (hash) {
    SignatureHash.DSTU7564 -> UA_SIGNATURE_HASH_DSTU7564
    SignatureHash.GOST34311 -> UA_SIGNATURE_HASH_GOST34311
}

/**
 * [text] as the UTF-8 the C ABI takes as a NUL-terminated string.
 *
 * Throws an [InvalidArgumentException] when it holds a NUL, which would cut the string short.
 */
internal fun nativeTextOf(text: String, what: String): ByteArray {
    if (text.contains(NUL)) {
        throw InvalidArgumentException("$what contains a NUL character.")
    }
    return text.toByteArray(Charsets.UTF_8)
}

/** [nativeTextOf] of a [password], zeroed once [body] returns or throws. */
internal inline fun <T> withNativePassword(password: String, body: (ByteArray) -> T): T {
    val bytes = nativeTextOf(password, "The password")
    try {
        return body(bytes)
    } finally {
        bytes.fill(0)
    }
}

/**
 * [value] as the `uint32_t` of the C ABI.
 *
 * Throws an [InvalidArgumentException] when it is negative, which the C ABI would read as a huge number.
 */
internal fun nativeUnsignedOf(value: Int, what: String): Int {
    if (value < 0) {
        throw InvalidArgumentException("$what is negative: $value.")
    }
    return value
}

private const val NUL = '\u0000'

// The constants of ua_cryptography.h.
private const val UA_SIGNATURE_FORMAT_CADES = 0
private const val UA_SIGNATURE_FORMAT_XADES = 1
private const val UA_SIGNATURE_FORMAT_ASIC_S = 2
private const val UA_SIGNATURE_FORMAT_ASIC_E = 3
private const val UA_SIGNATURE_LEVEL_B = 0
private const val UA_SIGNATURE_LEVEL_T = 1
private const val UA_SIGNATURE_LEVEL_LT = 2
private const val UA_SIGNATURE_HASH_DSTU7564 = 0
private const val UA_SIGNATURE_HASH_GOST34311 = 1
