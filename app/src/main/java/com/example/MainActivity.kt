package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NoteEntity
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NoteViewModel

sealed interface Screen {
    data object Home : Screen
    data class EditNote(val note: NoteEntity?) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecureNotesApp()
                }
            }
        }
    }
}

@Composable
fun SecureNotesApp(viewModel: NoteViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val unlockedNoteIds by viewModel.unlockedNoteIds.collectAsStateWithLifecycle()
    val defaultEmail by viewModel.defaultEmail.collectAsStateWithLifecycle()

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    notes = notes,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    categories = viewModel.categories,
                    unlockedNoteIds = unlockedNoteIds,
                    defaultEmail = defaultEmail,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    onAddNewNoteClick = { currentScreen = Screen.EditNote(null) },
                    onEditNoteClick = { note -> currentScreen = Screen.EditNote(note) },
                    onDeleteNoteClick = { note -> viewModel.deleteNote(note) },
                    onUnlockPassword = { note, password -> viewModel.unlockNoteWithPassword(note, password) },
                    onLockSession = { noteId -> viewModel.lockNoteSession(noteId) },
                    onSaveDefaultEmail = { email -> viewModel.saveDefaultEmail(email) }
                )
            }

            is Screen.EditNote -> {
                NoteEditSheet(
                    existingNote = screen.note,
                    defaultEmail = defaultEmail,
                    onBack = { currentScreen = Screen.Home },
                    onSave = { title, content, isLocked, passwordInput, passwordHint, category, recipientEmail, colorHex ->
                        viewModel.saveNote(
                            existingNote = screen.note,
                            title = title,
                            content = content,
                            isLocked = isLocked,
                            passwordInput = passwordInput,
                            passwordHint = passwordHint,
                            category = category,
                            recipientEmail = recipientEmail,
                            colorHex = colorHex,
                            onComplete = { currentScreen = Screen.Home }
                        )
                    }
                )
            }
        }
    }
}
