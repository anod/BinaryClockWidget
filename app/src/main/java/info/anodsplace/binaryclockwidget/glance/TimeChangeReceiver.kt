package info.anodsplace.binaryclockwidget.glance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.icu.util.TimeZone
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TimeChangeReceiver(
    private val context: Context,
    private val initialTimeZone: String?
) {

    private var calendar: Calendar = createTime(initialTimeZone)

    private fun createTime(timeZone: String?): Calendar {
        return if (timeZone != null) {
            Calendar.getInstance(TimeZone.getTimeZone(timeZone))
        } else {
            Calendar.getInstance()
        }
    }

    fun register(): Flow<Calendar> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (initialTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                   val timeZone = intent.getStringExtra(Intent.EXTRA_TIMEZONE);
                    calendar = createTime(timeZone)
                }
                calendar.setTimeInMillis(System.currentTimeMillis())
                trySendBlocking(calendar)
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        context.registerReceiver(receiver, intentFilter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}