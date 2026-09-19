package com.fitlog.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingImport by remember { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = state.exportedJson
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
        }
        viewModel.onExported()
    }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
            if (json != null) {
                viewModel.import(json)
            }
        }
    }

    LaunchedEffect(state.exportedJson) {
        if (state.exportedJson != null) {
            val name = "fitlog-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
            createDocument.launch(name)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Respaldo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Exportá o fusioná tus datos",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Volver") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXPORTAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.groups.forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = group.id in state.selectedGroups,
                                onCheckedChange = { viewModel.toggleGroup(group.id) },
                            )
                            Text(text = group.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Button(
                    onClick = viewModel::export,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Descargar respaldo") }
                Text(
                    text = "Los ejercicios propios que usan tus rutinas y entrenamientos se incluyen automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "IMPORTAR Y FUSIONAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    text = "Se fusiona por identificador: gana la fila más nueva, los borrados se propagan y las filas locales que no están en el archivo no se tocan.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (pendingImport) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                pendingImport = false
                                openDocument.launch(arrayOf("application/json"))
                            },
                        ) { Text("Elegir archivo") }
                        TextButton(onClick = { pendingImport = false }) { Text("Cancelar") }
                    }
                } else {
                    Button(
                        onClick = { pendingImport = true },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Importar respaldo") }
                }
            }
        }

        state.error?.let { message -> Text(text = message, color = MaterialTheme.colorScheme.error) }

        state.summary?.let { summary ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RESUMEN",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    val relevant = summary.filterValues {
                        it.inserted + it.updated + it.ignored + it.remapped > 0
                    }
                    if (relevant.isEmpty()) {
                        Text(
                            text = "No había filas nuevas ni más recientes: no se cambió nada.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    relevant.forEach { (table, counts) ->
                        Text(
                            text = "$table: +${counts.inserted} nuevas · ~${counts.updated} actualizadas · " +
                                "=${counts.ignored} ignoradas" +
                                if (counts.remapped > 0) " · ↻${counts.remapped} remapeadas" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = viewModel::clearSummary) { Text("Cerrar resumen") }
                }
            }
        }
    }
}
