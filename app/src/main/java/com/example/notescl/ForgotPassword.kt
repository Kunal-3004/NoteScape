package com.example.notescl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.notescl.databinding.ActivityForgotPasswordBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ForgotPassword : AppCompatActivity() {
    private  lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityForgotPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth= Firebase.auth

        binding.Forgot.setOnClickListener{
            val email=binding.ForgotEmail.text.toString()
            if(checkEmail()){
                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(this,"Email sent!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this,Login::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
        binding.imageButton.setOnClickListener{
            val intent = Intent(this,Login::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun checkEmail(): Boolean{
        val email=binding.ForgotEmail.text.toString()
        if(email==""){
            binding.ForgotEmail.error="This is a required field"
            return false
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.ForgotEmail.error="Check email format"
            return false
        }
        return true
    }
}