package com.example.notescl.database

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notescl.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Dao
interface NoteDao {
    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)


    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()


    @Query("SELECT  * FROM notes ORDER BY id DESC")
    fun getAllNotes():LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE:query")
    fun searchNote(query: String?): LiveData<List<Note>>


    @Transaction
    suspend fun retrieveUserNotesFromFirestore(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val notesCollection = firestore.collection("notes")

        try {
            val querySnapshot = notesCollection.whereEqualTo("userId", userId).get().await()

            val userNotes = mutableListOf<Note>()
            for (document in querySnapshot.documents) {
                val title = document.getString("title") ?: ""
                val content = document.getString("content") ?: ""
                val date = document.getString("date") ?: ""


                val noteId = document.id.hashCode()

                val note =
                    Note(noteId, title, content, date, userId)
                 userNotes.add(note)
            }
            insertNotes(userNotes)
        } catch (e: Exception) {
            Log.e("Firestore", "Error retrieving user notes: ${e.message}", e)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

}

