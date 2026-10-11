package io.github.marketkernel.uacryptography.example.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Label–value rows, the labels in a column of their own. */
@Composable
fun FactsTable(rows: List<Pair<String, String>>) {
    SelectionContainer {
        Column {
            for ((label, value) in rows) {
                Row(Modifier.padding(bottom = 6.dp)) {
                    Text(
                        label,
                        Modifier.width(LABEL_WIDTH).padding(end = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private val LABEL_WIDTH = 116.dp
