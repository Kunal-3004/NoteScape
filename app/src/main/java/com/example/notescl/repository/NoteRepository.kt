package com.example.notescl.repository

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.notescl.database.NoteDatabase
import com.example.notescl.model.Note
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NoteRepository(context: Context) {

    private val noteDao = NoteDatabase.invoke(context.applicationContext).getNoteDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteAllNotes() = noteDao.deleteAllNotes()

    fun getAllNotes() = noteDao.getAllNotes()
    fun searchNote(query: String?) = noteDao.searchNote("%$query%")

    suspend fun getNoteById(noteId: String): Note {
        return noteDao.getNoteById(noteId)
    }


    suspend fun retrieveUserNotesFromFirestore(userId: String) {
        noteDao.retrieveUserNotesFromFirestore(userId)
    }

}