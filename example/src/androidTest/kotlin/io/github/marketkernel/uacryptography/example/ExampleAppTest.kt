package io.github.marketkernel.uacryptography.example

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.marketkernel.uacryptography.example.trust.TrustRole
import io.github.marketkernel.uacryptography.example.trust.TrustStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun theBundledCertificatesLoadThePrivatBankBundleSplitIntoItsCertificates() {
        val store = TrustStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val entries = runBlocking {
            store.load()
            checkNotNull(store.entries.value)
        }
        val roots = entries.filter { it.role == TrustRole.ROOT }
        val intermediates = entries.filter { it.role == TrustRole.INTERMEDIATE }
        assertEquals(5, roots.size)
        assertTrue(roots.all { it.details.isCa })
        assertTrue(intermediates.size > 2)
        assertTrue(intermediates.any { it.source.endsWith(".p7b") })
    }

    @Test
    fun theAppStartsWithItsThreeTabsAndListsTheRoots() {
        composeRule.onNodeWithText("Verify").assertExists()
        composeRule.onNodeWithText("Sign").assertExists()
        composeRule.onNodeWithText("Certificates").performClick()
        composeRule.waitUntil(TIMEOUT_MILLISECONDS) {
            countNodesWithText("Trusted roots (5)") > 0
        }
    }

    @Test
    fun aSyntheticTestKeySignsAndTheSignatureVerifies() {
        composeRule.onNodeWithText("Sign").performClick()
        composeRule.onNodeWithText("Or create a synthetic test key (self-signed, for trying the app)").performClick()
        composeRule.waitUntil(TIMEOUT_MILLISECONDS) { countNodesWithText("Close the key") > 0 }
        composeRule.onNodeWithText("Тестовий підписувач").assertExists()
    }

    private fun countNodesWithText(text: String) = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size

    private companion object {
        const val TIMEOUT_MILLISECONDS = 10_000L
    }
}
