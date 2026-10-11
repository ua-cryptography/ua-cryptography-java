@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

/**
 * DSTU 7564:2014 "Kupyna" of [data]: [hashBits] / 8 bytes, as the standard writes them.
 *
 * [hashBits] is 256, 384 or 512; 256 is the hash of the national PKI.
 *
 * Throws an [InvalidArgumentException] for any other [hashBits].
 */
public fun dstu7564CalculateHash(data: ByteArray, hashBits: Int): ByteArray =
    NativeLibrary.checked().dstu7564CalculateHash(data, nativeUnsignedOf(hashBits, "The hash size"))
