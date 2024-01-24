package com.example.notescl.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.notescl.model.Note
import com.example.notescl.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class NoteViewModel(application: Application, noteRepository: NoteRepository) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository = NoteRepository(application)
    val dbFireStore = FirebaseFirestore.getInstance()


    fun addNote(note: Note) =
        viewModelScope.launch {
            noteRepository.insertNote(note)
        }

    fun deleteNote(note: Note) =
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }

    fun updateNote(note: Note) =
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }

    fun signOutAndClearData() = viewModelScope.launch {
        noteRepository.deleteAllNotes()
        FirebaseAuth.getInstance().signOut()
    }


    fun getAllNotes(): LiveData<List<Note>> = noteRepository.getAllNotes()
    fun searchNotes(query: String?) = noteRepository.searchNote(query)

    private val _notes = MutableLiveData<List<Note>>()
    fun setNotes(notesList: List<Note>) {
        _notes.value = notesList
    }

    fun updateNoteInFirestore(note: Note) = viewModelScope.launch {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val noteDocument = dbFireStore.collection("notes").document(note.id)

            noteDocument.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val updatedNote = mapOf(
                            "id" to note.id,
                            "title" to note.title,
                            "content" to note.content,
                            "date" to note.date,
                            "userId" to note.userId
                        )

                        noteDocument
                            .update(updatedNote)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Note updated successfully in Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating note in Firestore: ${e.message}", e)
                            }
                    } else {
                        Log.e("Firestore", "Document does not exist for note ID: ${note.id}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to check document existence: ${e.message}", e)
                }
        }
    }

    private var isFirestoreRetrieved = false

    fun  retrieveUserNotes(){
        val user = FirebaseAuth.getInstance().currentUser
        if(user!=null){
            val userId = user.uid
            dbFireStore.collection("notes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    val userNotes = mutableListOf<Note>()
                    for (document in result) {
                        val noteId=document.id
                        val title = document.getString("title")
                        val content = document.getString("content")
                        val date = document.getString("date")
                        val documentUserId = document.getString("userId")
                        val imageUrl = document.getString("imageUrl")


                        val note = Note(
                            noteId?:"",
                            title ?: "",
                            content ?: "",
                            date ?: "",
                            documentUserId,
                            imageUrl = imageUrl ?: ""
                        )
                        userNotes.add(note)
                    }
                    setNotes(userNotes)
                }

                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting notes: ${e.message}", e)
                }
            isFirestoreRetrieved=true
        }
    }

    fun retrieveAndPopulateUserNotes(userId: String) {
        viewModelScope.launch {
            noteRepository.retrieveUserNotesFromFirestore(userId)
        }
    }

    fun deleteNoteFromFirestore(noteId: String) {
        val notesCollection = dbFireStore.collection("notes")
        val noteDocument = notesCollection.document(noteId)

        noteDocument.delete()
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting document", e)
            }
    }

}

