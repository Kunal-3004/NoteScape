package com.example.notescl.viewModel

import android.app.Application
import android.app.DownloadManager.Query
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.notescl.model.Note
import com.example.notescl.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class NoteViewModel(app: Application, private val noteRepository: NoteRepository): AndroidViewModel(app) {


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


    fun getAllNotes() = noteRepository.getAllNotes()
    fun searchNotes(query: String?) = noteRepository.searchNote(query)

    private val _notes = MutableLiveData<List<Note>>()
    fun setNotes(notesList: List<Note>) {
        _notes.value = notesList
    }

    fun updateNoteInFirestore(note: Note) = viewModelScope.launch {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val dbFireStore = FirebaseFirestore.getInstance()
            val noteDocument = dbFireStore.collection("notes").document(note.id.toString())

            val updatedNote = mapOf(
                "title" to note.title,
                "content" to note.content,
                "date" to note.date
            )

            noteDocument
                .update(updatedNote)
                .addOnSuccessListener {
                    Log.d("Firestore", "Note updated successfully in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating note in Firestore: ${e.message}", e)
                }
        }
    }

    fun  retrieveUserNotes(){
        val user = FirebaseAuth.getInstance().currentUser
        if(user!=null){
            val userId = user.uid
            val dbFireStore = FirebaseFirestore.getInstance()
            dbFireStore.collection("notes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    val userNotes = mutableListOf<Note>()
                    for (document in result) {
                        val title = document.getString("title")
                        val content = document.getString("content")
                        val date = document.getString("date")

                        val note = Note(
                            0,
                            title ?: "",
                            content ?: "",
                            date ?: ""
                        )
                        userNotes.add(note)
                    }
                    setNotes(userNotes)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting notes: ${e.message}", e)
                }
        }
    }
}