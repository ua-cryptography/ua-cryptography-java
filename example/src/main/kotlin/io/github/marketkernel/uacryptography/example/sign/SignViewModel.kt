package io.github.marketkernel.uacryptography.example.sign

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.marketkernel.uacryptography.CertificateDetails
import io.github.marketkernel.uacryptography.InvalidArgumentException
import io.github.marketkernel.uacryptography.KeyStoreEntryKind
import io.github.marketkernel.uacryptography.KeyStoreRequest
import io.github.marketkernel.uacryptography.NamedBytes
import io.github.marketkernel.uacryptography.SignatureFormat
import io.github.marketkernel.uacryptography.SignatureHash
import io.github.marketkernel.uacryptography.SignatureLevel
import io.github.marketkernel.uacryptography.SigningOptions
import io.github.marketkernel.uacryptography.UaCryptographyException
import io.github.marketkernel.uacryptography.VerificationSummary
import io.github.marketkernel.uacryptography.createKeyStore
import io.github.marketkernel.uacryptography.example.ExampleApplication
import io.github.marketkernel.uacryptography.example.files.readNamedBytes
import io.github.marketkernel.uacryptography.getCertificate
import io.github.marketkernel.uacryptography.getCertificateDetails
import io.github.marketkernel.uacryptography.getKeyStoreEntries
import io.github.marketkernel.uacryptography.sign
import io.github.marketkernel.uacryptography.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An opened key store: the key that signs and its certificate. */
class OpenedKey(
    val fileName: String,
    val keyStore: ByteArray,
    val password: String,
    val aliases: List<String>,
    val alias: String,
    val certificate: ByteArray,
    val details: CertificateDetails,
    /** Made here by "Create a test key": its self-signed certificate is trusted for the check after signing. */
    val isSynthetic: Boolean,
) {
    /** Levels T and LT need the time-stamping service the certificate names. */
    val hasTimeStampService: Boolean get() = details.timeStampUrls.isNotEmpty()
}

/** What the Sign tab shows. */
data class SignState(
    val keyFileName: String? = null,
    val key: OpenedKey? = null,
    val keyFailure: String? = null,
    val isOpening: Boolean = false,
    val document: NamedBytes? = null,
    val format: SignatureFormat = SignatureFormat.CADES,
    val level: SignatureLevel = SignatureLevel.B,
    val hash: SignatureHash = SignatureHash.DSTU7564,
    val isDetached: Boolean = false,
    val isSigning: Boolean = false,
    val signFailure: String? = null,
    val signed: NamedBytes? = null,
    val report: VerificationSummary? = null,
) {
    /** The levels the key can sign at. */
    val levels: List<SignatureLevel>
        get() = if (key?.hasTimeStampService == true) SignatureLevel.entries else listOf(SignatureLevel.B)

    val canSign: Boolean get() = key != null && document != null && !isSigning
}

/** Sign: open a JKS key store, choose a document, sign; the result is verified on the spot. */
class SignViewModel(application: Application) : AndroidViewModel(application) {
    private val trustStore = (application as ExampleApplication).trustStore
    private val mutableState = MutableStateFlow(SignState())
    private var keyFile: NamedBytes? = null

    val state: StateFlow<SignState> = mutableState.asStateFlow()

    fun chooseKeyFile(uri: Uri) = viewModelScope.launch {
        try {
            val file = readNamedBytes(getApplication(), uri)
            keyFile = file
            mutableState.update { it.copy(keyFileName = file.name, keyFailure = null) }
        } catch (failure: IllegalStateException) {
            mutableState.update { it.copy(keyFailure = failure.message) }
        }
    }

    /** Opens the chosen store with [password]; the key [alias], else its first private key. */
    fun openKey(password: String, alias: String? = null) {
        val file = keyFile ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isOpening = true, keyFailure = null) }
            try {
                val key = openStore(file.name, file.content, password, alias, isSynthetic = false)
                mutableState.update { it.copy(key = key, level = defaultLevelOf(key), signed = null, report = null) }
            } catch (refusal: UaCryptographyException) {
                mutableState.update { it.copy(keyFailure = refusal.message) }
            } finally {
                mutableState.update { it.copy(isOpening = false) }
            }
        }
    }

    /** Switches to another key of the open store. */
    fun chooseAlias(alias: String) {
        val key = state.value.key ?: return
        viewModelScope.launch {
            try {
                val switched = openStore(key.fileName, key.keyStore, key.password, alias, key.isSynthetic)
                mutableState.update { it.copy(key = switched, level = defaultLevelOf(switched)) }
            } catch (refusal: UaCryptographyException) {
                mutableState.update { it.copy(keyFailure = refusal.message) }
            }
        }
    }

    fun createTestKey() = viewModelScope.launch {
        mutableState.update { it.copy(isOpening = true, keyFailure = null) }
        try {
            val created = createKeyStore(KeyStoreRequest(subject = TEST_KEY_SUBJECT), TEST_KEY_PASSWORD)
            val key = openStore(TEST_KEY_FILE_NAME, created.keyStore, TEST_KEY_PASSWORD, alias = null, isSynthetic = true)
            mutableState.update { it.copy(key = key, level = SignatureLevel.B, signed = null, report = null) }
        } catch (refusal: UaCryptographyException) {
            mutableState.update { it.copy(keyFailure = refusal.message) }
        } finally {
            mutableState.update { it.copy(isOpening = false) }
        }
    }

    fun closeKey() {
        keyFile = null
        mutableState.update { it.copy(keyFileName = null, key = null, signed = null, report = null) }
    }

    fun chooseDocument(uri: Uri) = viewModelScope.launch {
        try {
            val document = readNamedBytes(getApplication(), uri)
            mutableState.update { it.copy(document = document, signed = null, report = null, signFailure = null) }
        } catch (failure: IllegalStateException) {
            mutableState.update { it.copy(signFailure = failure.message) }
        }
    }

    fun setFormat(format: SignatureFormat) = mutableState.update { it.copy(format = format) }

    fun setLevel(level: SignatureLevel) = mutableState.update { it.copy(level = level) }

    fun setHash(hash: SignatureHash) = mutableState.update { it.copy(hash = hash) }

    fun setDetached(isDetached: Boolean) = mutableState.update { it.copy(isDetached = isDetached) }

    /** Signs the document as chosen, then verifies the new signature against the trust store. */
    fun signDocument() {
        val chosen = state.value
        val key = chosen.key ?: return
        val document = chosen.document ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isSigning = true, signFailure = null, signed = null, report = null) }
            try {
                val isDetachedCades = chosen.format == SignatureFormat.CADES && chosen.isDetached
                val certificates = trustStore.selectCertificates()
                val signature = sign(
                    key.keyStore,
                    key.password,
                    listOf(document),
                    alias = key.alias,
                    options = SigningOptions(
                        format = chosen.format,
                        level = chosen.level,
                        hash = chosen.hash,
                        isDetached = isDetachedCades,
                    ),
                    certificates = certificates,
                )
                val signed = NamedBytes(signatureFileName(document.name, chosen.format, isDetachedCades), signature)
                mutableState.update { it.copy(signed = signed) }
                val ownRoot = if (key.isSynthetic) listOf(key.certificate) else emptyList()
                val report = verify(
                    signature,
                    data = if (chosen.format == SignatureFormat.XADES || isDetachedCades) listOf(document) else emptyList(),
                    trustAnchors = trustStore.selectTrustAnchors() + ownRoot,
                    certificates = certificates,
                )
                mutableState.update { it.copy(report = report) }
            } catch (refusal: UaCryptographyException) {
                mutableState.update { it.copy(signFailure = refusal.message) }
            } finally {
                mutableState.update { it.copy(isSigning = false) }
            }
        }
    }

    private suspend fun openStore(
        fileName: String,
        keyStore: ByteArray,
        password: String,
        alias: String?,
        isSynthetic: Boolean,
    ): OpenedKey = withContext(Dispatchers.Default) {
        val aliases = getKeyStoreEntries(keyStore, password)
            .filter { it.kind == KeyStoreEntryKind.PRIVATE_KEY }
            .map { it.alias }
        if (aliases.isEmpty()) {
            throw InvalidArgumentException("The key store holds no private key.")
        }
        val chosenAlias = alias ?: aliases.first()
        val certificate = getCertificate(keyStore, password, chosenAlias)
        OpenedKey(fileName, keyStore, password, aliases, chosenAlias, certificate, getCertificateDetails(certificate), isSynthetic)
    }

    companion object {
        const val TEST_KEY_PASSWORD = "test-password"
        private const val TEST_KEY_SUBJECT = "CN=Тестовий підписувач,O=Synthetic Test,C=UA"
        private const val TEST_KEY_FILE_NAME = "test-key.jks"

        private fun defaultLevelOf(key: OpenedKey) = if (key.hasTimeStampService) SignatureLevel.T else SignatureLevel.B

        private fun signatureFileName(documentName: String, format: SignatureFormat, isDetached: Boolean) = when (format) {
            SignatureFormat.CADES -> if (isDetached) "$documentName.p7s" else "$documentName.sig"
            SignatureFormat.XADES -> "$documentName.xml"
            SignatureFormat.ASIC_S -> "$documentName.asics"
            SignatureFormat.ASIC_E -> "$documentName.asice"
        }
    }
}
