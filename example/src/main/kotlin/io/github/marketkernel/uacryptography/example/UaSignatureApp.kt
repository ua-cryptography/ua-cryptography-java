package io.github.marketkernel.uacryptography.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marketkernel.uacryptography.example.sign.SignScreen
import io.github.marketkernel.uacryptography.example.sign.SignViewModel
import io.github.marketkernel.uacryptography.example.trust.CertificatesScreen
import io.github.marketkernel.uacryptography.example.trust.CertificatesViewModel
import io.github.marketkernel.uacryptography.example.verify.VerifyScreen
import io.github.marketkernel.uacryptography.example.verify.VerifyViewModel

private val TAB_TITLES = listOf("Verify", "Sign", "Certificates")
private val BRAND_BLUE = Color(0xFF1F5FBF)
private val MAX_CONTENT_WIDTH = 960.dp

/** Verify and sign Ukrainian e-signatures: the three tabs of the Dart and TypeScript examples. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UaSignatureApp() {
    // The view models outlive a switch of tabs: a report stays while another tab is open.
    val verifyViewModel: VerifyViewModel = viewModel()
    val signViewModel: SignViewModel = viewModel()
    val certificatesViewModel: CertificatesViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = BRAND_BLUE)) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Ukrainian e-signature")
                                Text("DSTU 4145 · CAdES · XAdES · ASiC", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                    )
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        TAB_TITLES.forEachIndexed { index, title ->
                            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = MAX_CONTENT_WIDTH)
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    when (selectedTab) {
                        0 -> VerifyScreen(verifyViewModel)
                        1 -> SignScreen(signViewModel)
                        else -> CertificatesScreen(certificatesViewModel)
                    }
                }
            }
        }
    }
}
