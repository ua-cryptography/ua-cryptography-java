package io.github.marketkernel.uacryptography.example.verify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marketkernel.uacryptography.example.files.rememberFilesPicker
import io.github.marketkernel.uacryptography.example.report.VerificationReportView
import io.github.marketkernel.uacryptography.example.widgets.BannerTone
import io.github.marketkernel.uacryptography.example.widgets.ExpandableSection
import io.github.marketkernel.uacryptography.example.widgets.SectionCard
import io.github.marketkernel.uacryptography.example.widgets.StatusBanner

/** Verify: pick a signed file (and, for a detached signature, the signed files), see the report. */
@Composable
fun VerifyScreen(viewModel: VerifyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickFiles = rememberFilesPicker(viewModel::verifyFiles)
    Column {
        SectionCard {
            Text(
                "Choose a signed file",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                ".sig / .p7s (CAdES), .asice / .asics (ASiC), .xml (XAdES). For a detached signature, choose the " +
                    "signed file together with it.",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = pickFiles, Modifier.align(Alignment.CenterHorizontally), enabled = !state.isBusy) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose files…")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Check the current revocation status online", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Asks the OCSP servers the certificates name and downloads missing CA certificates; sends " +
                            "certificate serial numbers, nothing of the signature or the document.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = state.isOnline, onCheckedChange = viewModel::setOnline, enabled = !state.isBusy)
            }
        }
        if (state.isBusy) {
            StatusBanner(BannerTone.BUSY, "Verifying ${state.fileName}…")
        }
        state.failure?.let { StatusBanner(BannerTone.FAILURE, "Cannot verify ${state.fileName}", lines = listOf(it)) }
        state.report?.let { VerificationReportView(it, state.fileName.orEmpty()) }
        Spacer(Modifier.height(8.dp))
        ExpandableSection("What is checked here, and what is not") {
            Text(
                "• Checked: the DSTU 4145 signature and that it covers the document; the signer's certificate named " +
                    "in the signed attributes; the time-stamp; the certificate path up to a trusted root (ЦЗО, НБУ " +
                    "or one you add) with validity periods; OCSP responses embedded in the signature.\n" +
                    "• Online, when switched on: the current status of the certificates from the OCSP servers they " +
                    "name, and missing CA certificates from their caIssuers addresses.\n" +
                    "• Offline, gaps such as a missing revocation status are shown as warnings.\n" +
                    "• Files are not uploaded anywhere: everything happens on this device.",
            )
        }
    }
}
