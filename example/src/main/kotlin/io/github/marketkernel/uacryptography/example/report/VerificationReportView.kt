package io.github.marketkernel.uacryptography.example.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marketkernel.uacryptography.CheckOutcome
import io.github.marketkernel.uacryptography.SignatureSummary
import io.github.marketkernel.uacryptography.VerificationCheck
import io.github.marketkernel.uacryptography.VerificationSummary
import io.github.marketkernel.uacryptography.example.widgets.BannerTone
import io.github.marketkernel.uacryptography.example.widgets.ExpandableSection
import io.github.marketkernel.uacryptography.example.widgets.FactsTable
import io.github.marketkernel.uacryptography.example.widgets.SectionCard
import io.github.marketkernel.uacryptography.example.widgets.StatusBanner
import io.github.marketkernel.uacryptography.example.widgets.colorsOf
import io.github.marketkernel.uacryptography.example.widgets.formatDay
import io.github.marketkernel.uacryptography.example.widgets.formatTime

/** The verdict banner, the container's problems, and one card per signature. */
@Composable
fun VerificationReportView(report: VerificationSummary, fileName: String) {
    val signatures = report.signatures.filterNot { it.isContainer }
    val container = report.signatures.firstOrNull { it.isContainer }
    val tone = when {
        !report.isValid -> BannerTone.FAILURE
        report.hasWarnings -> BannerTone.WARNING
        else -> BannerTone.OK
    }
    val explanation = when (tone) {
        BannerTone.FAILURE -> "At least one check failed — see the marked items below."
        BannerTone.WARNING ->
            "Some things could not be established (usually the revocation status of a certificate); see \"Attention\"."
        else -> "Every check passed."
    }
    val signatureCount = "${signatures.size} signature${if (signatures.size == 1) "" else "s"}"
    Column {
        StatusBanner(
            tone = tone,
            title = if (report.isValid) "The signature is valid" else "The signature is NOT valid",
            lines = listOf(explanation, "$fileName — ${report.format}, $signatureCount"),
        )
        Spacer(Modifier.height(16.dp))
        if (container != null && container.checks.any { it.status != CheckOutcome.PASSED }) {
            SectionCard(title = "Container") { CheckList(container.checks) }
        }
        for (summary in signatures) {
            SignatureCard(summary)
        }
    }
}

/** One signature: who signed, when, with what, and every check. */
@Composable
private fun SignatureCard(summary: SignatureSummary) {
    val signer = summary.signer
    val problems = summary.checks.filter { it.status != CheckOutcome.PASSED }
    val (pillLabel, pillTone) = when {
        !summary.isValid -> "INVALID" to BannerTone.FAILURE
        summary.hasWarnings -> "VALID, WITH WARNINGS" to BannerTone.WARNING
        else -> "VALID" to BannerTone.OK
    }
    val format = listOfNotNull(summary.level, summary.packaging, summary.digestAlgorithm?.name).joinToString(", ")
    val facts = buildList {
        if (format.isNotEmpty()) add("Format" to format)
        summary.signatureAlgorithm?.let { add("Algorithm" to it.name) }
        summary.signingTime?.let { add("Signed at" to "${formatTime(it)} (as the signer's computer says)") }
        summary.timestampTime?.let { add("Time-stamp" to "${formatTime(it)} (time-stamp, verified)") }
        signer?.subjectAttribute("O")?.let { add("Organization" to it) }
        signer?.subjectAttribute("SERIALNUMBER")?.let { add("Identifier" to it) }
        if (signer != null) {
            add("Issued by" to signer.issuerName)
            add("Certificate valid" to "${formatDay(signer.validFrom)} – ${formatDay(signer.validTo)}")
        }
        add("Signature" to summary.title)
    }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                signer?.subjectName ?: summary.title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Pill(pillLabel, pillTone)
        }
        Spacer(Modifier.height(12.dp))
        FactsTable(facts)
        if (problems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("ATTENTION", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            CheckList(problems)
        }
        ExpandableSection("All checks (${summary.checks.size})") { CheckList(summary.checks) }
        if (summary.references.isNotEmpty()) {
            ExpandableSection("Signed digests") {
                FactsTable(summary.references.map { it.name to "${it.digestAlgorithm.name} ${it.digest}" })
            }
        }
    }
}

/** Checks with their marks: ✓ passed, ! warning, ✗ failed. */
@Composable
private fun CheckList(checks: List<VerificationCheck>) {
    SelectionContainer {
        Column {
            for (check in checks) {
                Row(Modifier.padding(vertical = 4.dp)) {
                    val (mark, tone) = when (check.status) {
                        CheckOutcome.PASSED -> "✓" to BannerTone.OK
                        CheckOutcome.WARNING -> "!" to BannerTone.WARNING
                        CheckOutcome.FAILED -> "✗" to BannerTone.FAILURE
                    }
                    Text(mark, Modifier.width(20.dp), color = colorsOf(tone).second, fontWeight = FontWeight.Bold)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(check.name) }
                            append(": ${check.details}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, tone: BannerTone) {
    val (background, foreground) = colorsOf(tone)
    Text(
        label,
        Modifier.background(background, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = foreground,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}
