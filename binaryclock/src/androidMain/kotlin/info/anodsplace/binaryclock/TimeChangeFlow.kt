package info.anodsplace.binaryclock

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

internal fun timeChangeFlow(context: Context): Flow<Calendar> = callbackFlow {
    trySendBlocking(Calendar.getInstance())
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent) {
            val cal = if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
                val tz = intent.getStringExtra(Intent.EXTRA_TIMEZONE)
                if (tz != null) Calendar.getInstance(TimeZone.getTimeZone(tz))
                else Calendar.getInstance()
            } else {
                Calendar.getInstance()
            }
            cal.timeInMillis = System.currentTimeMillis()
            trySendBlocking(cal)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    context.registerReceiver(receiver, filter)
    awaitClose { context.unregisterReceiver(receiver) }
}
