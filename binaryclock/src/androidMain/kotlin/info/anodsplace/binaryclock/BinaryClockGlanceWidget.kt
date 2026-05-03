// Lint false positive: ColorProvider(Color) is public API, but lint confuses it with the restricted ColorProvider(@ColorRes Int) overload
// https://issuetracker.google.com/issues/324087645
@file:SuppressLint("RestrictedApi")

package info.anodsplace.binaryclock

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BinaryClockGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val config = BinaryClockWidgetConfigKeys.fromPreferences(currentState<Preferences>())
            val now = LocalTime.now()
            BinaryClockWidgetContent(
                digits = if (config.showSeconds) {
                    BinaryClockDigits.timeDigits(hour = now.hour, minute = now.minute, second = now.second)
                } else {
                    BinaryClockDigits.timeDigits(hour = now.hour, minute = now.minute)
                },
                showBitLabels = config.showBitLabels,
                showHmsLabels = config.showHmsLabels,
                appWidgetId = appWidgetId,
            )
        }
    }
}

class BinaryClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BinaryClockGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BinaryClockRefreshScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        BinaryClockRefreshScheduler.cancel(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_REFRESH_BINARY_CLOCK
            || action == Intent.ACTION_TIME_CHANGED
            || action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val applicationContext = context.applicationContext
                    glanceAppWidget.updateAll(applicationContext)
                    BinaryClockRefreshScheduler.scheduleNext(applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Re-schedule alarm on every update (app reinstall, system restart, periodic update)
        BinaryClockRefreshScheduler.scheduleNext(context)
    }
}

// Max possible value per digit column: H tens(2), H ones(9), M tens(5), M ones(9), S tens(5), S ones(9)
private val maxDigitValues = listOf(2, 9, 5, 9, 5, 9)
private val labels = listOf("H", "M", "S")
private val labelFontSize = 10.sp

@Composable
fun BinaryClockWidgetContent(
    digits: List<Int>,
    showBitLabels: Boolean = true,
    showHmsLabels: Boolean = true,
    appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
) {
    val isCompactMode = LocalSize.current.width < REGULAR_LAYOUT_MINIMUM_WIDTH
    val dotSize = if (isCompactMode) 6.dp else 8.dp
    val groupGap = if (isCompactMode) 6.dp else 10.dp
    val digitGap = if (isCompactMode) 3.dp else 4.dp
    val rowGap = if (isCompactMode) 4.dp else 6.dp
    val quadSize = dotSize * 2 // total size of one quad dot (2 sub-dots, no inner gap)
    val activeColor = GlanceTheme.colors.onSurfaceVariant
    val inactiveColor = GlanceTheme.colors.surfaceVariant
    val pairCount = digits.size / 2

    val clickModifier = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        GlanceModifier.clickable(
            actionStartActivity(
                ComponentName("info.anodsplace.binaryclockwidget", "info.anodsplace.binaryclockwidget.MainActivity"),
                actionParametersOf(
                    ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) to appWidgetId,
                ),
            )
        )
    } else {
        GlanceModifier
    }

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.background)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .then(clickModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until pairCount) {
                    if (i > 0) {
                        Spacer(modifier = GlanceModifier.width(groupGap))
                    }
                    DigitPair(
                        digits[i * 2], digits[i * 2 + 1],
                        maxDigitValues[i * 2], maxDigitValues[i * 2 + 1],
                        if (showHmsLabels) labels[i] else null,
                        dotSize, digitGap, rowGap, quadSize, activeColor, inactiveColor,
                    )
                }
                if (showBitLabels) {
                    Spacer(modifier = GlanceModifier.width(groupGap))
                    BitLabelsColumn(quadSize = quadSize, rowGap = rowGap, showHmsLabels = showHmsLabels)
                }
            }
        }
    }
}

@Composable
private fun DigitPair(
    digit1: Int, digit2: Int,
    maxValue1: Int, maxValue2: Int,
    label: String?,
    dotSize: Dp, digitGap: Dp, rowGap: Dp, quadSize: Dp,
    activeColor: ColorProvider, inactiveColor: ColorProvider,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BinaryDigitColumn(digit1, maxValue1, dotSize, rowGap, quadSize, activeColor, inactiveColor)
            Spacer(modifier = GlanceModifier.width(digitGap))
            BinaryDigitColumn(digit2, maxValue2, dotSize, rowGap, quadSize, activeColor, inactiveColor)
        }
        if (label != null) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = labelFontSize,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun BinaryDigitColumn(
    digit: Int, maxValue: Int,
    dotSize: Dp, rowGap: Dp, quadSize: Dp,
    activeColor: ColorProvider, inactiveColor: ColorProvider,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BinaryClockDigits.digitBits(digit).forEachIndexed { bitIndex, active ->
            val bitValue = BinaryClockDigits.bitValues[bitIndex]
            if (bitValue <= maxValue) {
                QuadDot(active = active, dotSize = dotSize, activeColor = activeColor, inactiveColor = inactiveColor)
            } else {
                Spacer(modifier = GlanceModifier.width(quadSize).height(quadSize))
            }
            if (bitIndex < 3) {
                Spacer(modifier = GlanceModifier.height(rowGap))
            }
        }
    }
}

// A 2×2 grid of small rounded boxes forming a bigger square-ish dot
@Composable
private fun QuadDot(active: Boolean, dotSize: Dp, activeColor: ColorProvider, inactiveColor: ColorProvider) {
    val color = if (active) activeColor else inactiveColor
    val cornerRadius = dotSize / 2
    Column {
        Row {
            Box(modifier = GlanceModifier.width(dotSize).height(dotSize).background(color).cornerRadius(cornerRadius)) {}
            Box(modifier = GlanceModifier.width(dotSize).height(dotSize).background(color).cornerRadius(cornerRadius)) {}
        }
        Row {
            Box(modifier = GlanceModifier.width(dotSize).height(dotSize).background(color).cornerRadius(cornerRadius)) {}
            Box(modifier = GlanceModifier.width(dotSize).height(dotSize).background(color).cornerRadius(cornerRadius)) {}
        }
    }
}

@Composable
private fun BitLabelsColumn(quadSize: Dp, rowGap: Dp, showHmsLabels: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BinaryClockDigits.bitValues.forEachIndexed { index, value ->
            Text(
                text = value.toString(),
                modifier = GlanceModifier.height(quadSize),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = labelFontSize,
                    textAlign = TextAlign.Center,
                ),
            )
            if (index < 3) {
                Spacer(modifier = GlanceModifier.height(rowGap))
            }
        }
        if (showHmsLabels) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "",
                style = TextStyle(fontSize = labelFontSize),
            )
        }
    }
}

private val REGULAR_LAYOUT_MINIMUM_WIDTH = 200.dp

private const val ACTION_REFRESH_BINARY_CLOCK = "info.anodsplace.binaryclock.action.REFRESH"
private const val MINUTE_MILLIS = 60_000L
private const val INEXACT_REFRESH_WINDOW_MILLIS = 10_000L

private object BinaryClockRefreshScheduler {
    fun scheduleNext(context: Context) {
        val nextMinute = System.currentTimeMillis().let { now ->
            now - (now % MINUTE_MILLIS) + MINUTE_MILLIS
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val refreshIntent = pendingIntent(context)
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC, nextMinute, refreshIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC, nextMinute, INEXACT_REFRESH_WINDOW_MILLIS, refreshIntent)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BinaryClockWidgetReceiver::class.java)
            .setAction(ACTION_REFRESH_BINARY_CLOCK)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
