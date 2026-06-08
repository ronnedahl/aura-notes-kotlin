package dev.peterbot.auranotes.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.peterbot.auranotes.R
import dev.peterbot.auranotes.data.local.NoteEntity
import dev.peterbot.auranotes.speech.SpeechState
import dev.peterbot.auranotes.ui.theme.AuraNotesTheme
import dev.peterbot.auranotes.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stateful entry point: connects the [NoteViewModel] to the stateless content
 * and owns the things that need an Android context — the RECORD_AUDIO permission
 * request and the "is speech available" check — keeping the content previewable.
 */
@Composable
fun NoteScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel(),
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val micDeniedMessage = stringResource(R.string.mic_permission_denied)
    val notAvailableMessage = stringResource(R.string.speech_not_available)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            scope.launch { snackbarHostState.showSnackbar(micDeniedMessage) }
        }
    }

    val onRecordClick: () -> Unit = {
        when {
            !viewModel.isSpeechAvailable() ->
                scope.launch { snackbarHostState.showSnackbar(notAvailableMessage) }

            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED ->
                viewModel.startRecording()

            else ->
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    NoteScreenContent(
        notes = notes,
        recordingState = recordingState,
        snackbarHostState = snackbarHostState,
        onRecordClick = onRecordClick,
        onStopRecording = viewModel::stopRecording,
        onCancelRecording = viewModel::cancelRecording,
        onAddNote = viewModel::addNote,
        onDeleteNote = viewModel::deleteNote,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteScreenContent(
    notes: List<NoteEntity>,
    recordingState: SpeechState,
    snackbarHostState: SnackbarHostState,
    onRecordClick: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.add_text_note),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onRecordClick) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.record_note),
                )
            }
        },
    ) { innerPadding ->
        if (notes.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = notes, key = { it.id }) { note ->
                    NoteCard(note = note, onDelete = { onDeleteNote(note) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text ->
                onAddNote(text)
                showAddDialog = false
            },
        )
    }

    if (recordingState !is SpeechState.Idle) {
        RecordingDialog(
            state = recordingState,
            onStop = onStopRecording,
            onCancel = onCancelRecording,
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                end = 8.dp,
                bottom = 8.dp,
            ),
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimestamp(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_note),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_notes_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_notes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_note)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Voice-recording overlay. Shows the live transcription while listening and a
 * Stop button to finish; an error state offers only a dismiss. The note itself
 * is saved by the ViewModel when the final result arrives.
 */
@Composable
private fun RecordingDialog(
    state: SpeechState,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    val isError = state is SpeechState.Error

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(
                    if (isError) R.string.recording_error else R.string.recording_title,
                ),
            )
        },
        text = {
            val body = when (state) {
                is SpeechState.Listening -> state.partialText
                is SpeechState.Result -> state.text
                else -> stringResource(R.string.recording_hint)
            }
            if (!isError) Text(body)
        },
        confirmButton = {
            if (isError) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.stop))
                }
            }
        },
        dismissButton = if (isError) {
            null
        } else {
            {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun NoteScreenPreview() {
    AuraNotesTheme {
        NoteScreenContent(
            notes = listOf(
                NoteEntity(id = 1, text = "Köp mjölk och bröd", createdAt = 1_717_000_000_000),
                NoteEntity(id = 2, text = "Idé: rösta-först anteckningsapp", createdAt = 1_717_100_000_000),
            ),
            recordingState = SpeechState.Idle,
            snackbarHostState = SnackbarHostState(),
            onRecordClick = {},
            onStopRecording = {},
            onCancelRecording = {},
            onAddNote = {},
            onDeleteNote = {},
        )
    }
}
