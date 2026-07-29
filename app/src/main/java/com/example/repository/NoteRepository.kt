package com.example.repository

import com.example.data.NoteDao
import com.example.data.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNoteById(id: Int): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdSync(id: Int): NoteEntity? = noteDao.getNoteByIdSync(id)

    suspend fun insert(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun update(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Int) = noteDao.deleteNoteById(id)
}
