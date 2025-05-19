package com.project.taima

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.project.taima.com.project.taima.SummaryActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Views
        val customerSection = findViewById<LinearLayout>(R.id.sectionCustomer)
        val toolsSection = findViewById<LinearLayout>(R.id.sectionTools)
        val routeSection = findViewById<LinearLayout>(R.id.sectionRoute)
        val summarySection = findViewById<LinearLayout>(R.id.sectionSummary)
        val logoutText = findViewById<TextView>(R.id.logoutText)
        logoutText.setOnClickListener {
            logout()
        }

        // ClickListeners
        customerSection.setOnClickListener {
            val intent = Intent(this@HomeActivity, CustomerActivity::class.java)
            startActivity(intent)
        }

        routeSection.setOnClickListener {
            val intent = Intent(this@HomeActivity, RouteActivity::class.java)
            startActivity(intent)
        }

        summarySection.setOnClickListener {
            val intent = Intent(this@HomeActivity, SummaryActivity::class.java)
            startActivity(intent)
        }

        toolsSection.setOnClickListener {
            val intent = Intent(this@HomeActivity, ToolsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Método para cerrar sesión
     */
    private fun logout() {
        auth.signOut()

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            remove("sessionToken")
            remove("sessionDate")
            apply()
        }

        val intent = Intent(this@HomeActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Log.d("Logout", "Usuario deslogueado correctamente")
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
