package io.github.marketkernel.uacryptography.example.verify

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.marketkernel.uacryptography.UaCryptographyException
import io.github.marketkernel.uacryptography.VerificationSummary
import io.github.marketkernel.uacryptography.example.ExampleApplication
import io.github.marketkernel.uacryptography.example.files.readNamedBytes
import io.github.marketkernel.uacryptography.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the Verify tab shows. */
data class VerifyState(
    val isOnline: Boolean = false,
    val isBusy: Boolean = false,
    val fileName: String? = null,
    val report: VerificationSummary? = null,
    val failure: String? = null,
)

/** Verify: a signed file (and, for a detached signature, the signed files) against the trust store. */
class VerifyViewModel(application: Application) : AndroidViewModel(application) {
    private val trustStore = (application as ExampleApplication).trustStore
    private val mutableState = MutableStateFlow(VerifyState())

    val state: StateFlow<VerifyState> = mutableState.asStateFlow()

    fun setOnline(isOnline: Boolean) = mutableState.update { it.copy(isOnline = isOnline) }

    /** Verifies the picked files: the one of a signature format is the signature, the others its data. */
    fun verifyFiles(uris: List<Uri>) {
        if (uris.isEmpty()) {
            return
        }
        viewModelScope.launch {
            val files = try {
                uris.map { readNamedBytes(getApplication(), it) }
            } catch (failure: IllegalStateException) {
                mutableState.update { it.copy(report = null, failure = failure.message) }
                return@launch
            }
            val signature = files.firstOrNull { it.name.substringAfterLast('.').lowercase() in SIGNATURE_EXTENSIONS }
                ?: files.first()
            val data = files.filterNot { it === signature }
            mutableState.update { it.copy(isBusy = true, fileName = signature.name, report = null, failure = null) }
            try {
                val report = verify(
                    signature.content,
                    data = data,
                    trustAnchors = trustStore.selectTrustAnchors(),
                    certificates = trustStore.selectCertificates(),
                    isOnline = state.value.isOnline,
                )
                mutableState.update { it.copy(report = report) }
            } catch (refusal: UaCryptographyException) {
                mutableState.update { it.copy(failure = refusal.message) }
            } finally {
                mutableState.update { it.copy(isBusy = false) }
            }
        }
    }

    private companion object {
        val SIGNATURE_EXTENSIONS = setOf("sig", "p7s", "p7m", "asice", "sce", "asics", "scs", "xml")
    }
}
