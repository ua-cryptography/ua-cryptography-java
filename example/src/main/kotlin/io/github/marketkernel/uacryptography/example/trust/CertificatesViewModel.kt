package io.github.marketkernel.uacryptography.example.trust

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.marketkernel.uacryptography.UaCryptographyException
import io.github.marketkernel.uacryptography.example.ExampleApplication
import io.github.marketkernel.uacryptography.example.files.readNamedBytes
import io.github.marketkernel.uacryptography.example.widgets.BannerTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How the last addition of certificates ended. */
data class AdditionMessage(val tone: BannerTone, val text: String)

/** Certificates: the trust store's entries, adding files of certificates and removing added ones. */
class CertificatesViewModel(application: Application) : AndroidViewModel(application) {
    private val trustStore = (application as ExampleApplication).trustStore
    private val mutableMessage = MutableStateFlow<AdditionMessage?>(null)

    val entries: StateFlow<List<TrustedCertificate>?> = trustStore.entries
    val message: StateFlow<AdditionMessage?> = mutableMessage.asStateFlow()

    fun addFiles(uris: List<Uri>, role: TrustRole) {
        if (uris.isEmpty()) {
            return
        }
        viewModelScope.launch {
            var addedCount = 0
            val failedNames = mutableListOf<String>()
            for (uri in uris) {
                val file = runCatching { readNamedBytes(getApplication(), uri) }.getOrNull()
                try {
                    addedCount += trustStore.addFile(checkNotNull(file).content, role)
                } catch (_: IllegalArgumentException) {
                    failedNames += file?.name ?: uri.toString()
                } catch (_: IllegalStateException) {
                    failedNames += file?.name ?: uri.toString()
                } catch (_: UaCryptographyException) {
                    failedNames += file?.name ?: uri.toString()
                }
            }
            val plural = if (addedCount == 1) "" else "s"
            mutableMessage.value = if (failedNames.isEmpty()) {
                AdditionMessage(BannerTone.OK, "Added $addedCount certificate$plural.")
            } else {
                AdditionMessage(BannerTone.FAILURE, "No certificate in ${failedNames.joinToString()}; added $addedCount.")
            }
        }
    }

    fun remove(certificate: TrustedCertificate) = viewModelScope.launch { trustStore.remove(certificate) }
}
