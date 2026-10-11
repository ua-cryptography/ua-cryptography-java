package io.github.marketkernel.uacryptography.example.trust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marketkernel.uacryptography.example.files.rememberFilesPicker
import io.github.marketkernel.uacryptography.example.widgets.SectionCard
import io.github.marketkernel.uacryptography.example.widgets.StatusBanner
import io.github.marketkernel.uacryptography.example.widgets.formatDay
import io.github.marketkernel.uacryptography.getVersion

/** Certificates: the trusted roots and intermediate certificates verification relies on, and adding more. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pickRoots = rememberFilesPicker { viewModel.addFiles(it, TrustRole.ROOT) }
    val pickIntermediates = rememberFilesPicker { viewModel.addFiles(it, TrustRole.INTERMEDIATE) }
    Column {
        SectionCard(title = "Add certificates") {
            Text("DER or PEM .cer / .crt, or a bundle .p7b. Added certificates are kept on this device.")
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = pickRoots) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Trusted root…")
                }
                FilledTonalButton(onClick = pickIntermediates) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Intermediate…")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Only add a root you have checked yourself. Intermediates are a provider's CA, TSP or OCSP certificates.",
                style = MaterialTheme.typography.bodySmall,
            )
            message?.let { StatusBanner(it.tone, it.text) }
        }
        val loaded = entries
        if (loaded == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        } else {
            val roots = loaded.filter { it.role == TrustRole.ROOT }
            val intermediates = loaded.filter { it.role == TrustRole.INTERMEDIATE }
            SectionCard(title = "Trusted roots (${roots.size})") {
                Text("A signature is trusted when its certificate path ends at one of these.")
                for (entry in roots) {
                    CertificateRow(entry, onRemove = viewModel::remove)
                }
            }
            SectionCard(title = "Intermediate certificates (${intermediates.size})") {
                Text(
                    "Used to complete paths and to find time-stamping services that a signature does not carry (a " +
                        "PrivatBank .sig has neither its CA's nor its TSP server's certificate).",
                )
                for (entry in intermediates) {
                    CertificateRow(entry, onRemove = viewModel::remove)
                }
            }
        }
        Text(
            "ua-cryptography-android · native library ${getVersion()}",
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CertificateRow(entry: TrustedCertificate, onRemove: (TrustedCertificate) -> Unit) {
    val details = entry.details
    val issuedBy = if (details.issuer == details.subject) "self-signed" else "issued by ${details.issuerName}"
    val origin = if (entry.isBuiltIn) "built in (${entry.source})" else "added"
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(details.subjectName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "$issuedBy · ${formatDay(details.validFrom)} – ${formatDay(details.validTo)} · " +
                    "${details.signatureAlgorithm.name} · $origin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!entry.isBuiltIn) {
            IconButton(onClick = { onRemove(entry) }) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
        }
    }
}
