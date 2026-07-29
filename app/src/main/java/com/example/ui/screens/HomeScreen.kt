package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NoteEntity
import com.example.ui.components.DefaultEmailDialog
import com.example.ui.components.NoteCard
import com.example.ui.components.PasswordUnlockDialog
import com.example.ui.components.SendGmailModal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealAccent
import com.example.utils.EmailHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<NoteEntity>,
    searchQuery: String,
    selectedCategory: String,
    categories: List<String>,
    unlockedNoteIds: Set<Int>,
    defaultEmail: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddNewNoteClick: () -> Unit,
    onEditNoteClick: (NoteEntity) -> Unit,
    onDeleteNoteClick: (NoteEntity) -> Unit,
    onUnlockPassword: (NoteEntity, String) -> Boolean,
    onLockSession: (Int) -> Unit,
    onSaveDefaultEmail: (String) -> Unit
) {
    val context = LocalContext.current

    var pendingUnlockNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingSendGmailNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingDeleteNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val totalCount = notes.size
    val lockedCount = notes.count { it.isLocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "دفترچه یادداشت رمزی",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "رمز اختصاصی هر یادداشت + ارسال به جیمیل",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات جیمیل",
                            tint = NavyPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNewNoteClick,
                icon = { Icon(Icons.Default.Add, contentDescription = "افزودن") },
                text = { Text("یادداشت جدید", fontWeight = FontWeight.Bold) },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_note_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("جستجو در عنوان یا متن یادداشت‌ها...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "جستجو", tint = NavyPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "پاکسازی")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    focusedLabelColor = NavyPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_notes_input")
            )

            // Filter Categories Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = (selectedCategory == category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
                        label = {
                            Text(
                                text = when (category) {
                                    "رمزدار" -> "🔒 رمزدار"
                                    "عادی" -> "📄 عادی"
                                    else -> category
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_$category")
                    )
                }
            }

            // Stats / Info Banner
            Surface(
                color = NavyPrimary.copy(alpha = 0.04f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مجموع یادداشت‌ها: $totalCount عدد",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = NavyPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "رمزدار: $lockedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notes List or Empty State
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_notes_1785358217368),
                            contentDescription = "دفترچه خالی است",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "هیچ یادداشتی با این مشخصات پیدا نشد!" else "دفترچه یادداشت شما خالی است",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "برای ایجاد یادداشت جدید و تنظیم رمز اختصاصی یا ارسال به جیمیل، روی دکمه + کلیک کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val isUnlocked = unlockedNoteIds.contains(note.id)

                        NoteCard(
                            note = note,
                            isUnlocked = isUnlocked,
                            onCardClick = {
                                if (note.isLocked && !isUnlocked) {
                                    pendingUnlockNote = note
                                } else {
                                    onEditNoteClick(note)
                                }
                            },
                            onSendGmailClick = {
                                if (note.isLocked && !isUnlocked) {
                                    pendingUnlockNote = note
                                } else {
                                    pendingSendGmailNote = note
                                }
                            },
                            onEditClick = {
                                if (note.isLocked && !isUnlocked) {
                                    pendingUnlockNote = note
                                } else {
                                    onEditNoteClick(note)
                                }
                            },
                            onDeleteClick = {
                                pendingDeleteNote = note
                            },
                            onLockToggleClick = {
                                if (note.isLocked && isUnlocked) {
                                    onLockSession(note.id)
                                } else if (note.isLocked) {
                                    pendingUnlockNote = note
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Password Unlock Dialog
    pendingUnlockNote?.let { note ->
        PasswordUnlockDialog(
            note = note,
            onDismiss = { pendingUnlockNote = null },
            onUnlock = { passwordInput ->
                val success = onUnlockPassword(note, passwordInput)
                if (success) {
                    pendingUnlockNote = null
                }
                success
            }
        )
    }

    // Gmail Send Dialog
    pendingSendGmailNote?.let { note ->
        SendGmailModal(
            note = note,
            defaultEmail = defaultEmail,
            onDismiss = { pendingSendGmailNote = null },
            onSend = { recipientEmail ->
                EmailHelper.sendToGmail(
                    context = context,
                    recipientEmail = recipientEmail,
                    subject = note.title,
                    body = note.content
                )
                pendingSendGmailNote = null
            }
        )
    }

    // Confirm Delete Dialog
    pendingDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text("حذف یادداشت", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف یادداشت «${note.title}» اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNoteClick(note)
                        pendingDeleteNote = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Default Email Settings Dialog
    if (showSettingsDialog) {
        DefaultEmailDialog(
            currentEmail = defaultEmail,
            onDismiss = { showSettingsDialog = false },
            onSave = { newEmail ->
                onSaveDefaultEmail(newEmail)
                showSettingsDialog = false
            }
        )
    }
}
