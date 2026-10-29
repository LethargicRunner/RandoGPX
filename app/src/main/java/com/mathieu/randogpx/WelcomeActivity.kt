package com.mathieu.randogpx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Écran d'accueil (V2.0.1) : affiché à CHAQUE lancement, choix explicite
 * entre suivre un GPX et partir en rando libre.
 */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<Button>(R.id.btnFollowGpx).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).putExtra("mode", "gpx")
            )
        }
        findViewById<Button>(R.id.btnFreeHike).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).putExtra("mode", "libre")
            )
        }
    }
}
