package io.github.marketkernel.uacryptography.example.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A labelled choice of one of [options], each shown by [nameOf]. */
@Composable
fun <T> ChoiceField(label: String, selected: T, options: List<T>, nameOf: (T) -> String, onSelected: (T) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { isOpen = true }, Modifier.fillMaxWidth()) {
                Text(nameOf(selected), Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
                for (option in options) {
                    DropdownMenuItem(
                        text = { Text(nameOf(option)) },
                        onClick = {
                            isOpen = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
    }
}
