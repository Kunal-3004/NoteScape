package com.example.notescl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SplashScreen : AppCompatActivity() {

    var firstPressTime:Long=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            val currentUser=FirebaseAuth.getInstance().currentUser
            if(currentUser!=null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else{
                val intent = Intent(this, Login::class.java)
                startActivity(intent)
                finish()
            }
        },2000)
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