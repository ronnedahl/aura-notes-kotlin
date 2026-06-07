package dev.peterbot.auranotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.peterbot.auranotes.ui.NoteScreen
import dev.peterbot.auranotes.ui.theme.AuraNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraNotesTheme {
                NoteScreen()
            }
        }
    }
}
