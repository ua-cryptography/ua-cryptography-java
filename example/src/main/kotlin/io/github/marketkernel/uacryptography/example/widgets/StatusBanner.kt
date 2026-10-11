package io.github.marketkernel.uacryptography.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The tone of a banner: success, a warning that leaves the result standing, a failure, or work in progress. */
enum class BannerTone { OK, WARNING, FAILURE, BUSY }

/** The background and foreground of a tone. */
fun colorsOf(tone: BannerTone): Pair<Color, Color> = when (tone) {
    BannerTone.OK -> Color(0xFFE6F4EA) to Color(0xFF1E6B34)
    BannerTone.WARNING -> Color(0xFFFFF4DC) to Color(0xFF8A5A00)
    BannerTone.FAILURE -> Color(0xFFFDE7E7) to Color(0xFFA11B1B)
    BannerTone.BUSY -> Color(0xFFECEEF2) to Color(0xFF1B1C1F)
}

/** A coloured banner with a mark, a headline, optional lines below it and an optional action. */
@Composable
fun StatusBanner(
    tone: BannerTone,
    title: String,
    modifier: Modifier = Modifier,
    lines: List<String> = emptyList(),
    action: (@Composable () -> Unit)? = null,
) {
    val (background, foreground) = colorsOf(tone)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(background, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        when (tone) {
            BannerTone.BUSY -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            BannerTone.FAILURE -> Icon(Icons.Filled.Warning, contentDescription = null, tint = foreground)
            else -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = foreground)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.align(Alignment.CenterVertically)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = foreground, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Column {
                    for (line in lines) {
                        Text(line, Modifier.padding(top = 4.dp), color = Color(0xFF1B1C1F))
                    }
                }
            }
            if (action != null) {
                Column(Modifier.padding(top = 8.dp)) { action() }
            }
        }
    }
}
