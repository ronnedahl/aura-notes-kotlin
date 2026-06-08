package dev.peterbot.auranotes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.peterbot.auranotes.ui.NoteScreen
import dev.peterbot.auranotes.ui.theme.AuraNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // White status-bar icons read well over the blue top bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AuraNotesTheme {
                NoteScreen()
            }
        }
    }
}
