package io.github.marketkernel.uacryptography

/**
 * A refusal of the native library: one subclass per status of the C ABI, carrying the library's message.
 *
 * Sealed, so a `when` over the subclasses is exhaustive. An invalid signature is not an exception: [verify] reports it.
 */
public sealed class UaCryptographyException(
    /** The library's explanation, in English. */
    override val message: String,
) : Exception(message)

/** An argument is of a wrong length, not UTF-8, or a name or number the library does not know. */
public class InvalidArgumentException(message: String) : UaCryptographyException(message)

/** A key store, certificate or signature does not parse. */
public class UnreadableInputException(message: String) : UaCryptographyException(message)

/** The key store or its key does not open with the password. */
public class WrongPasswordException(message: String) : UaCryptographyException(message)

/** An authenticated ciphertext or a wrapped key is not the one tagged. */
public class AuthenticationFailedException(message: String) : UaCryptographyException(message)

/** A time-stamping, OCSP or certificate download service failed. */
public class ServiceFailureException(message: String) : UaCryptographyException(message)

/** A defect of the native library: the call panicked. */
public class InternalException(message: String) : UaCryptographyException(message)
