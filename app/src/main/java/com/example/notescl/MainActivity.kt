package com.example.notescl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.notescl.database.NoteDatabase
import com.example.notescl.repository.NoteRepository
import com.example.notescl.viewModel.NoteViewModel
import com.example.notescl.viewModel.NoteViewModelFactory
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    lateinit var noteViewModel: NoteViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViewModel()
        noteViewModel.retrieveUserNotes()
        FirebaseApp.initializeApp(this)
    }


    private fun setupViewModel(){
        val noteRepository=NoteRepository(NoteDatabase(this))
        val viewModelProviderFactory=NoteViewModelFactory(application,noteRepository)
        noteViewModel=ViewModelProvider(this,viewModelProviderFactory)[NoteViewModel::class.java]
    }
}