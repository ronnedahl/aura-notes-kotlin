package dev.peterbot.auranotes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.peterbot.auranotes.ui.NoteScreen
import dev.peterbot.auranotes.ui.theme.AuraNotesTheme
import dev.peterbot.auranotes.widget.AuraNotesWidgetProvider

class MainActivity : ComponentActivity() {

    /** Set when launched from the widget; tells NoteScreen to start recording. */
    private var autoStartRecording by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh launch (not a config-change recreation) so rotating
        // mid-note doesn't restart recording.
        autoStartRecording = savedInstanceState == null && intent.startsRecording()

        // White status-bar icons read well over the blue top bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            AuraNotesTheme {
                NoteScreen(
                    autoStartRecording = autoStartRecording,
                    onAutoStartHandled = { autoStartRecording = false },
                )
            }
        }
    }

    /** The app is already open and the widget is tapped again. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.startsRecording()) autoStartRecording = true
    }

    private fun Intent.startsRecording(): Boolean =
        action == AuraNotesWidgetProvider.ACTION_START_RECORDING
}
