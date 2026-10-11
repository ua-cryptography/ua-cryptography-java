package io.github.marketkernel.uacryptography.example.trust

import android.content.Context
import androidx.core.content.edit
import io.github.marketkernel.uacryptography.CertificateDetails
import io.github.marketkernel.uacryptography.getCertificateDetails
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What a certificate is trusted for. */
enum class TrustRole(
    /** The folder of `assets/certificates` the bundled ones of this role are in. */
    val folder: String,
) {
    /** A path must end at one of these. */
    ROOT("roots"),

    /** Completes paths: CAs, time-stamping and OCSP services. */
    INTERMEDIATE("intermediates"),
}

/** A certificate verification relies on. */
class TrustedCertificate(
    val der: ByteArray,
    val details: CertificateDetails,
    val role: TrustRole,
    val isBuiltIn: Boolean,
    /** The asset file it came from, or "added". */
    val source: String,
)

/**
 * The certificates verification relies on: the public roots and intermediates bundled in `assets/certificates`, and
 * those the user added, kept in shared preferences as `<role>:<base64 DER>` lines — public certificates, nothing
 * secret.
 */
class TrustStore(private val context: Context) {
    private val mutex = Mutex()
    private val mutableEntries = MutableStateFlow<List<TrustedCertificate>?>(null)

    /** Every certificate, the bundled ones first; null until [load] has read them. */
    val entries: StateFlow<List<TrustedCertificate>?> = mutableEntries.asStateFlow()

    /** Reads the bundled certificates and the stored ones. */
    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val loaded = mutableListOf<TrustedCertificate>()
            // Roots first: the PrivatBank bundle among the intermediates carries some of the roots too.
            for (role in TrustRole.entries) {
                val folder = "$ASSET_ROOT/${role.folder}"
                for (asset in context.assets.list(folder).orEmpty().sorted()) {
                    val bytes = context.assets.open("$folder/$asset").use { it.readBytes() }
                    for (der in splitCertificates(bytes)) {
                        loaded.addIfNew(der, role, isBuiltIn = true, source = asset)
                    }
                }
            }
            for (line in readStoredLines()) {
                // A damaged stored entry is dropped at the next save.
                runCatching {
                    val (roleName, encoded) = line.split(ROLE_SEPARATOR, limit = 2)
                    loaded.addIfNew(Base64.getDecoder().decode(encoded), TrustRole.valueOf(roleName), false, ADDED)
                }
            }
            mutableEntries.value = loaded
        }
    }

    /** The trusted roots, DER, once loaded. */
    suspend fun selectTrustAnchors(): List<ByteArray> = awaitEntries().filter { it.role == TrustRole.ROOT }.map { it.der }

    /** The intermediate certificates, DER, once loaded. */
    suspend fun selectCertificates(): List<ByteArray> =
        awaitEntries().filter { it.role == TrustRole.INTERMEDIATE }.map { it.der }

    /**
     * Adds the certificates of a file with [role]; returns how many were new. A known certificate added as a root
     * becomes one.
     */
    suspend fun addFile(bytes: ByteArray, role: TrustRole): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = awaitEntries().toMutableList()
            var addedCount = 0
            for (der in splitCertificates(bytes)) {
                val existing = updated.indexOfFirst { it.der.contentEquals(der) }
                if (existing < 0) {
                    updated.addIfNew(der, role, isBuiltIn = false, source = ADDED)
                    addedCount++
                } else if (role == TrustRole.ROOT && updated[existing].role != TrustRole.ROOT) {
                    val entry = updated[existing]
                    updated[existing] = TrustedCertificate(entry.der, entry.details, TrustRole.ROOT, false, ADDED)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                save(updated)
            }
            addedCount
        }
    }

    /** Removes an added certificate; bundled ones stay. */
    suspend fun remove(certificate: TrustedCertificate) = withContext(Dispatchers.IO) {
        mutex.withLock {
            save(awaitEntries().filterNot { !it.isBuiltIn && it === certificate })
        }
    }

    private suspend fun awaitEntries(): List<TrustedCertificate> = entries.filterNotNull().first()

    private fun readStoredLines(): List<String> =
        preferences().getString(STORAGE_KEY, null)?.lines()?.filter(String::isNotBlank).orEmpty()

    private fun save(updated: List<TrustedCertificate>) {
        val lines = updated.filterNot { it.isBuiltIn }.joinToString("\n") {
            "${it.role.name}$ROLE_SEPARATOR${Base64.getEncoder().encodeToString(it.der)}"
        }
        preferences().edit { putString(STORAGE_KEY, lines) }
        mutableEntries.value = updated
    }

    private fun preferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun MutableList<TrustedCertificate>.addIfNew(der: ByteArray, role: TrustRole, isBuiltIn: Boolean, source: String) {
        if (none { it.der.contentEquals(der) }) {
            add(TrustedCertificate(der, getCertificateDetails(der), role, isBuiltIn, source))
        }
    }

    private companion object {
        const val ASSET_ROOT = "certificates"
        const val PREFERENCES_NAME = "trust_store"
        const val STORAGE_KEY = "certificates"
        const val ROLE_SEPARATOR = ":"
        const val ADDED = "added"
    }
}
