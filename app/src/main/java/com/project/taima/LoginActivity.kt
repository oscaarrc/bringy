package com.project.taima

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Check if there is already an active session
        checkActiveSession()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()

            checkDniExists(username) { exists ->
                if (exists) {
                    signInAnonymously(username) {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun checkDniExists(dni: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("User")
            .whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun signInAnonymously(dni: String, onSuccess: () -> Unit) {
        auth.signInAnonymously()
            .addOnSuccessListener {
                // Saves the session as active on Firestore with a token
                saveSessionToken(dni)
                // Saves the login date in SharedPreferences
                saveActiveSession()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
            }
    }

    // Saves the session token on Firestore
    private fun saveSessionToken(dni: String) {
        val db = FirebaseFirestore.getInstance()

        val token = System.currentTimeMillis().toString() // Unique token

        db.collection("User")
            .whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.first()
                    val userId = userDoc.id


                    db.collection("User")
                        .document(userId)
                        .update("sessionToken", token)
                        .addOnSuccessListener {
                            saveTokenToSharedPreferences(token)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar el token", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun saveTokenToSharedPreferences(token: String) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("sessionToken", token)
            putLong("sessionDate", System.currentTimeMillis())  // Saves the login date
            apply()
        }
    }

    private fun saveActiveSession() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putLong("sessionDate", System.currentTimeMillis())
            apply()
        }
    }


    private fun checkActiveSession() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val sessionToken = sharedPreferences.getString("sessionToken", null)
        val sessionDate = sharedPreferences.getLong("sessionDate", 0L)
        val thirtyDays = 30 * 24 * 60 * 60 * 1000L

        if (sessionToken != null && System.currentTimeMillis() - sessionDate <= thirtyDays) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
