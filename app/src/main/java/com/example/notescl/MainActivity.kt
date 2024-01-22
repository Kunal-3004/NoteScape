package com.example.notescl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
        FirebaseApp.initializeApp(this)
        Glide.with(this).applyDefaultRequestOptions(RequestOptions())
    }


    private fun setupViewModel(){
        val noteRepository=NoteRepository(this)
        val viewModelProviderFactory=NoteViewModelFactory(application,noteRepository)
        noteViewModel=ViewModelProvider(this,viewModelProviderFactory)[NoteViewModel::class.java]
    }
}