package info.anodsplace.binaryclockwidget.glance

import android.content.Context
import android.icu.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Surfaces
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import info.anodsplace.binaryclockwidget.BinaryTime
import kotlin.math.max
import kotlin.math.min

class AppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/widget/TextClock.java;l=574?q=TextClock&ss=android%2Fplatform%2Fsuperproject%2Fmain

        val timeChangeReceiver = TimeChangeReceiver(context, initialTimeZone = null)

        // In this method, load data needed to render the AppWidget.
        // Use `withContext` to switch to another thread for long running
        // operations.
        provideContent {
            GlanceTheme() {
                val time by timeChangeReceiver.register().collectAsState(initial = Calendar.getInstance())
                val hour = time.get(Calendar.HOUR_OF_DAY)
                val minute = time.get(Calendar.MINUTE)
                val second = time.get(Calendar.SECOND)
                WidgetContent(hour, minute, second)
            }
        }
    }

}

private val numberSize = 20.dp

@Composable
private fun WidgetContent(hour: Int, minute: Int, second: Int) {
    val hourParts = remember(hour) { BinaryTime.convert(hour) }
    val minuteParts = remember(minute) {  BinaryTime.convert(minute) }
    val secondParts = remember(hour) { BinaryTime.convert(second) }
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackgroundModifier()
            .appWidgetBackgroundCornerRadius(),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleNumberColumn(numberArray = hourParts[0], maxNumber = 2)
        Spacer(modifier = GlanceModifier.width(1.dp))
        SingleNumberColumn(numberArray = hourParts[1])

        Spacer(modifier = GlanceModifier.width(4.dp))
        SingleNumberColumn(numberArray = minuteParts[0])
        Spacer(modifier = GlanceModifier.width(1.dp))
        SingleNumberColumn(numberArray = minuteParts[1])

        Spacer(modifier = GlanceModifier.width(4.dp))
        SingleNumberColumn(numberArray = secondParts[0])
        Spacer(modifier = GlanceModifier.width(1.dp))
        SingleNumberColumn(numberArray = secondParts[1])
        HelpColumn()
    }
}

@Composable
fun HelpColumn() {
    Column {
        arrayOf(8,4,2,1).forEach {
            Box(
                modifier = GlanceModifier
                    .size(numberSize),
                contentAlignment = Alignment.Center,
                content = {
                    Text(
                        text = "$it",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SingleNumberColumn(numberArray: ByteArray, maxNumber: Int = numberArray.size) {
    val space = max(numberArray.size - maxNumber, 0)
    var current = space
    val enabled = 1.toByte()
    Column {
        if (space > 0) {
            Spacer(modifier = GlanceModifier
                .height(numberSize * space))
        }
        while (current < numberArray.size) {
            val value = numberArray[current];
            Box(
                modifier = GlanceModifier
                    .size(numberSize)
                    .background(if (value == enabled) GlanceTheme.colors.primary else GlanceTheme.colors.primaryContainer)
                    .appWidgetInnerCornerRadius(),
                contentAlignment = Alignment.Center,
                content = {
//                    Text(
//                        text = "$value",
//                        style = TextStyle(
//                            color = ColorProvider(Color.Black),
//                            fontSize = 12.sp
//                        )
//                    )
                }
            )
            current++
        }
    }
}

@Composable
fun GlanceModifier.appWidgetBackgroundModifier(): GlanceModifier {
    return this.fillMaxSize()
        .padding(12.dp)
        .appWidgetBackground()
        .background(GlanceTheme.colors.background)
        .appWidgetBackgroundCornerRadius()
}

fun GlanceModifier.appWidgetBackgroundCornerRadius(): GlanceModifier {
    return cornerRadius(android.R.dimen.system_app_widget_background_radius)
}

fun GlanceModifier.appWidgetInnerCornerRadius(): GlanceModifier {
    return cornerRadius(android.R.dimen.system_app_widget_inner_radius)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@androidx.glance.preview.Preview(Surfaces.APP_WIDGET)
@Composable
fun WidgetPreview() {
    GlanceTheme() {
        WidgetContent(hour = 21, minute = 59, second = 6)
    }
}