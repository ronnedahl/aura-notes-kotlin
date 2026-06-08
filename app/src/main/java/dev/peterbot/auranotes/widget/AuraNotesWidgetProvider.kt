package dev.peterbot.auranotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.peterbot.auranotes.MainActivity
import dev.peterbot.auranotes.R

/**
 * Home-screen widget for one-tap voice notes.
 *
 * Recording needs an Activity (RECORD_AUDIO permission + SpeechRecognizer), so
 * the widget can't record on its own — tapping it launches [MainActivity] with
 * [ACTION_START_RECORDING], and the app starts recording immediately.
 */
class AuraNotesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_aura_notes).apply {
            setOnClickPendingIntent(R.id.widget_root, recordPendingIntent(context))
        }
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    private fun recordPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_START_RECORDING
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val ACTION_START_RECORDING = "dev.peterbot.auranotes.action.START_RECORDING"
    }
}
