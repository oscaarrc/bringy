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

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Verificar si ya hay una sesión activa
        checkActiveSession()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()

            // Comprobar si el DNI existe en la base de datos
            checkDniExists(username) { exists ->
                if (exists) {
                    // Iniciar sesión anónima
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
                // Guardar la sesión como activa en Firestore con un token
                saveSessionToken(dni)
                // Guardar la fecha de inicio de sesión en SharedPreferences
                saveActiveSession()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
            }
    }

    // Guardar el token de sesión en Firestore
    private fun saveSessionToken(dni: String) {
        val db = FirebaseFirestore.getInstance()

        val token = System.currentTimeMillis().toString() // Generamos un token único

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
            putLong("sessionDate", System.currentTimeMillis())  // Guardamos la fecha de inicio de sesión
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
