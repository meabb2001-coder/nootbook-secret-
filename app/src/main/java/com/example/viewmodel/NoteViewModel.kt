package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.NoteEntity
import com.example.repository.NoteRepository
import com.example.security.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("همه")
    val unlockedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val defaultEmail = MutableStateFlow(prefs.getString("default_email", "moseabb@gmail.com") ?: "moseabb@gmail.com")

    val categories = listOf("همه", "رمزدار", "عادی", "شخصی", "کاری", "مهم", "ایده‌ها")

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        checkAndSeedSampleNotes()
    }

    private fun checkAndSeedSampleNotes() {
        viewModelScope.launch {
            val existing = repository.allNotes.first()
            if (existing.isEmpty()) {
                // Seed 1: Welcome & Gmail Share note
                repository.insert(
                    NoteEntity(
                        title = "خوش آمدید به دفترچه یادداشت رمزی 📝",
                        content = "این یک یادداشت نمونه است.\nشما می‌توانید متن هر یادداشت را به سادگی با لمس دکمه «ارسال به جیمیل» به آدرس جیمیل دلخواه ارسال کنید.\n\nهمچنین برای امنیت بیشتر، می‌توانید برای هر یادداشت یک رمز عبور مستقل تنظیم کنید!",
                        isLocked = false,
                        category = "شخصی",
                        recipientEmail = "moseabb@gmail.com",
                        colorHex = "#FFF7ED"
                    )
                )

                // Seed 2: Locked Note sample with password "1234"
                val salt = PasswordHasher.generateSalt()
                val hash = PasswordHasher.hashPassword("1234", salt)
                repository.insert(
                    NoteEntity(
                        title = "اطلاعات مهم و رمزدار 🔒",
                        content = "تبریک! شما موفق شدید این یادداشت محافظت‌شده را باز کنید.\nاطلاعات سری و یادداشت‌های خصوصی خود را با رمز عبور مجزا حفظ کنید.\nرمز این یادداشت نمونه: 1234 است.",
                        isLocked = true,
                        passwordHash = hash,
                        passwordSalt = salt,
                        passwordHint = "عدد چهار رقمی ساده (1234)",
                        category = "مهم",
                        recipientEmail = "moseabb@gmail.com",
                        colorHex = "#FAF5FF"
                    )
                )
            }
        }
    }

    val notes: StateFlow<List<NoteEntity>> = combine(
        repository.allNotes,
        searchQuery,
        selectedCategory
    ) { allNotesList, query, cat ->
        allNotesList.filter { note ->
            val matchesSearch = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    (note.content.contains(query, ignoreCase = true) && (!note.isLocked || unlockedNoteIds.value.contains(note.id)))

            val matchesCategory = when (cat) {
                "همه" -> true
                "رمزدار" -> note.isLocked
                "عادی" -> !note.isLocked
                else -> note.category == cat
            }

            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        selectedCategory.value = category
    }

    fun saveDefaultEmail(email: String) {
        defaultEmail.value = email
        prefs.edit().putString("default_email", email).apply()
    }

    fun saveNote(
        existingNote: NoteEntity?,
        title: String,
        content: String,
        isLocked: Boolean,
        passwordInput: String,
        passwordHint: String,
        category: String,
        recipientEmail: String,
        colorHex: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var finalHash = existingNote?.passwordHash ?: ""
            var finalSalt = existingNote?.passwordSalt ?: ""

            if (isLocked) {
                if (passwordInput.isNotBlank()) {
                    finalSalt = PasswordHasher.generateSalt()
                    finalHash = PasswordHasher.hashPassword(passwordInput, finalSalt)
                }
            } else {
                finalHash = ""
                finalSalt = ""
            }

            val noteToSave = NoteEntity(
                id = existingNote?.id ?: 0,
                title = title.ifBlank { "بدون عنوان" },
                content = content,
                isLocked = isLocked,
                passwordHash = finalHash,
                passwordSalt = finalSalt,
                passwordHint = if (isLocked) passwordHint else "",
                category = category,
                recipientEmail = recipientEmail,
                colorHex = colorHex,
                createdAt = existingNote?.createdAt ?: now,
                updatedAt = now
            )

            if (existingNote == null || existingNote.id == 0) {
                val newId = repository.insert(noteToSave).toInt()
                if (isLocked && passwordInput.isNotBlank()) {
                    // Auto unlock for creator during this session
                    unlockedNoteIds.value = unlockedNoteIds.value + newId
                }
            } else {
                repository.update(noteToSave)
                if (isLocked && passwordInput.isNotBlank()) {
                    unlockedNoteIds.value = unlockedNoteIds.value + existingNote.id
                }
            }
            onComplete()
        }
    }

    fun unlockNoteWithPassword(note: NoteEntity, passwordInput: String): Boolean {
        val isCorrect = PasswordHasher.verifyPassword(passwordInput, note.passwordHash, note.passwordSalt)
        if (isCorrect) {
            unlockedNoteIds.value = unlockedNoteIds.value + note.id
        }
        return isCorrect
    }

    fun lockNoteSession(noteId: Int) {
        unlockedNoteIds.value = unlockedNoteIds.value - noteId
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
            unlockedNoteIds.value = unlockedNoteIds.value - note.id
        }
    }
}
