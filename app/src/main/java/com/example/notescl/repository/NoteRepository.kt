package com.example.notescl.repository

import android.app.Application
import android.content.Context
import com.example.notescl.database.NoteDatabase
import com.example.notescl.model.Note

class NoteRepository(context: Context) {

    // Initialize the database using the provided context
    private val noteDao = NoteDatabase.invoke(context.applicationContext).getNoteDao()

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteAllNotes() = noteDao.deleteAllNotes()

    fun getAllNotes() = noteDao.getAllNotes()
    fun searchNote(query: String?) = noteDao.searchNote("%$query%")
}