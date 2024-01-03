package com.example.notescl

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.notescl.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        binding.signupButton.setOnClickListener{
            val  email=binding.signupEmail.text.toString()
            val password=binding.signupPassword.text.toString()
            if(checkAllField()){
                auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
                    if(it.isSuccessful){
                        auth.signOut()
                        Toast.makeText(this,"Account created successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this,Login::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else{
                        Log.e("error",it.exception.toString())
                    }
                }
            }
        }
        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun checkAllField(): Boolean{
        val email=binding.signupEmail.text.toString()
        if(email==""){
            binding.signupEmail.error="This is a required field"
            return false
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.signupEmail.error="Check email format"
            return false
        }
        if(binding.signupPassword.text.toString()==""){
            binding.signupPassword.error="This is a required field"
            return false
        }
        if(binding.signupPassword.length() <= 6) {
            binding.signupPassword.error = "Password should atleast 8 characters long"
            return false
        }
        if(binding.signupConfirmPassword.text.toString()=="") {
            binding.signupConfirmPassword.error = "This is a required field"
            return false
        }
        if(binding.signupConfirmPassword.text.toString()!=binding.signupPassword.text.toString()){
            binding.signupPassword.error="Password do not match"
            return false
        }
        return true
    }
}