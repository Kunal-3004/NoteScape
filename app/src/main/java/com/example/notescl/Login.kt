package com.example.notescl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.notescl.databinding.ActivityLoginBinding
import com.example.notescl.repository.NoteRepository
import com.example.notescl.viewModel.NoteViewModel
import com.example.notescl.viewModel.NoteViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    var firstPressTime:Long=0

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteRepository:NoteRepository
    private lateinit var noteViewModelFactory: NoteViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        noteRepository= NoteRepository(application)
        noteViewModelFactory = NoteViewModelFactory(application,noteRepository)
        noteViewModel = ViewModelProvider(this, noteViewModelFactory).get(NoteViewModel::class.java)


        binding.LoginButton.setOnClickListener {
            val email=binding.Email.text.toString()
            val password=binding.Password.text.toString()
            if(checkAllField()){
                auth.signInWithEmailAndPassword(email,password).addOnCompleteListener {
                    if(it.isSuccessful){
                        noteViewModel.retrieveUserNotes()
                        Toast.makeText(this,"Successfully Login", Toast.LENGTH_SHORT).show()
                        val intent= Intent(this,MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else{
                        Log.e("error",it.exception.toString())
                    }
                }
            }
        }
        binding.CreateAccountButton.setOnClickListener{
            val intent = Intent(this,SignUp::class.java)
            startActivity(intent)
            finish()
        }
        binding.ForgotPassword.setOnClickListener{
            val intent = Intent(this,ForgotPassword::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkAllField(): Boolean{
        val email=binding.Email.text.toString()
        if(email==""){
            binding.Email.error="This is a required field"
            return false
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.Email.error="Check email format"
            return false
        }
        if(binding.Password.text.toString()==""){
            binding.Password.error="This is a required field"
            return false
        }
        if(binding.Password.length() <= 6) {
            binding.Password.error = "Password should atleast 8 characters long"
            return false
        }
        return true
    }
    override fun onBackPressed() {
        if(firstPressTime+1000>System.currentTimeMillis()){
            super.onBackPressed()
        }else{
            Toast.makeText(baseContext,"Press Back Twice to Exit", Toast.LENGTH_SHORT).show()
        }
        firstPressTime=System.currentTimeMillis()
    }
}