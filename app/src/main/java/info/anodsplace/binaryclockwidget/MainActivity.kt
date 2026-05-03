package info.anodsplace.binaryclockwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.state.updateAppWidgetState
import info.anodsplace.binaryclock.BinaryClockDigits
import info.anodsplace.binaryclock.BinaryClockGlanceWidget
import info.anodsplace.binaryclock.BinaryClockWidgetConfig
import info.anodsplace.binaryclock.BinaryClockWidgetConfigKeys
import info.anodsplace.binaryclock.BinaryClockWidgetContent
import info.anodsplace.binaryclock.BinaryClockWidgetReceiver
import info.anodsplace.binaryclockwidget.ui.theme.BinaryClockWidgetTheme
import java.time.LocalTime
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(appWidgetId = intent?.
            extras?.
            getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)

        val isConfigMode = viewModel.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        // Widget config: default to canceled until user confirms
        setResult(RESULT_CANCELED)

        setContent {
            BinaryClockWidgetTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isConfigMode) {
                        val config by viewModel.widgetConfig.collectAsState()
                        WidgetConfigScreen(
                            appWidgetId = viewModel.appWidgetId,
                            initialConfig = config,
                            onConfirm = { newConfig ->
                                saveConfigAndFinish(viewModel.appWidgetId, newConfig)
                            },
                        )
                    } else {
                        val uiState by viewModel.uiState.collectAsState()
                        MainScreen(
                            uiState = uiState,
                            onProviderClicked = viewModel::onProviderClicked
                        )
                    }
                }
            }
        }
    }

    private fun saveConfigAndFinish(appWidgetId: Int, config: BinaryClockWidgetConfig) {
        MainScope().launch {
            saveWidgetConfig(appWidgetId, config)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    private suspend fun saveWidgetConfig(appWidgetId: Int, config: BinaryClockWidgetConfig) {
        val glanceId = GlanceAppWidgetManager(this@MainActivity).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(this@MainActivity, glanceId) { prefs ->
            prefs[BinaryClockWidgetConfigKeys.showBitLabels] = config.showBitLabels
            prefs[BinaryClockWidgetConfigKeys.showHmsLabels] = config.showHmsLabels
            prefs[BinaryClockWidgetConfigKeys.showSeconds] = config.showSeconds
        }
        BinaryClockGlanceWidget().update(this@MainActivity, glanceId)
        // Reschedule refresh alarm (interval depends on showSeconds config)
        BinaryClockGlanceWidget.rescheduleRefresh(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun MainScreen(uiState: MainViewState, onProviderClicked: (ProviderData) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 600.dp
        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                WidgetPreviewSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                WidgetInstancesSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    uiState = uiState,
                    onProviderClicked = onProviderClicked,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                WidgetPreviewSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                WidgetInstancesSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    uiState = uiState,
                    onProviderClicked = onProviderClicked,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetPreviewSection(modifier: Modifier = Modifier, onSaveConfig: ((BinaryClockWidgetConfig) -> Unit)? = null) {
    var showBitLabels by remember { mutableStateOf(true) }
    var showHmsLabels by remember { mutableStateOf(true) }
    var showSeconds by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showSeconds) {
        while (true) {
            val now = System.currentTimeMillis()
            val interval = if (showSeconds) 1000L else 60_000L
            val next = now - (now % interval) + interval
            kotlinx.coroutines.delay(next - now)
            tick = System.currentTimeMillis()
        }
    }

    fun currentConfig() = BinaryClockWidgetConfig(showBitLabels = showBitLabels, showHmsLabels = showHmsLabels, showSeconds = showSeconds)

    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val triggerRecomposition = tick
    val now = LocalTime.now()
    val digits = if (showSeconds) {
        BinaryClockDigits.timeDigits(now.hour, now.minute, now.second)
    } else {
        BinaryClockDigits.timeDigits(now.hour, now.minute)
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val previewWidth = minOf(maxWidth, 360.dp)
        val previewHeight = previewWidth * 0.55f
        val size = DpSize(previewWidth, previewHeight)

        val remoteViews by produceState<android.widget.RemoteViews?>(
            initialValue = null, digits, size, showBitLabels, showHmsLabels, showSeconds,
        ) {
            value = GlanceRemoteViews().compose(
                context = context,
                size = size,
            ) {
                BinaryClockWidgetContent(
                    digits = digits,
                    showBitLabels = showBitLabels,
                    showHmsLabels = showHmsLabels,
                )
            }.remoteViews
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
            remoteViews?.let { rv ->
                AndroidView(
                    modifier = Modifier
                        .width(previewWidth)
                        .height(previewHeight),
                    factory = { ctx ->
                        val density = ctx.resources.displayMetrics.density
                        val widthPx = (previewWidth.value * density).toInt()
                        val heightPx = (previewHeight.value * density).toInt()
                        FrameLayout(ctx).apply {
                            val child = rv.apply(ctx, this)
                            child.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
                            addView(child)
                        }
                    },
                    update = { frameLayout ->
                        val density = frameLayout.context.resources.displayMetrics.density
                        val widthPx = (previewWidth.value * density).toInt()
                        val heightPx = (previewHeight.value * density).toInt()
                        frameLayout.removeAllViews()
                        val child = rv.apply(frameLayout.context, frameLayout)
                        child.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
                        frameLayout.addView(child)
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ConfigToggle(
                label = "Show seconds",
                checked = showSeconds,
                onCheckedChange = { showSeconds = it; onSaveConfig?.invoke(currentConfig()) },
            )
            ConfigToggle(
                label = "Show bit labels",
                checked = showBitLabels,
                onCheckedChange = { showBitLabels = it; onSaveConfig?.invoke(currentConfig()) },
            )
            ConfigToggle(
                label = "Show hint",
                checked = showHmsLabels,
                onCheckedChange = { showHmsLabels = it; onSaveConfig?.invoke(currentConfig()) },
            )
        }
    }
}

@Composable
private fun WidgetInstancesSection(
    modifier: Modifier = Modifier,
    uiState: MainViewState,
    onProviderClicked: (ProviderData) -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        Text(
            "Installed Widgets",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.providers) {
                ShowProvider(it, onProviderClicked = onProviderClicked)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun ShowProvider(providerData: ProviderData, onProviderClicked: (ProviderData) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onProviderClicked(providerData)
            }
    ) {
        Text(providerData.provider.simpleName, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            providerData.appWidgets.forEachIndexed { index, widget ->
                ShowAppWidget(index, widget)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun ShowAppWidget(index: Int, widgetDesc: AppWidgetDesc) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Instance ${index + 1}")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            widgetDesc.sizes.sortedBy { it.width.value * it.height.value }
                .forEachIndexed { index, size ->
                    Text(
                        String.format(
                            "Size ${index + 1}: %.0f dp x %.0f dp",
                            size.width.value,
                            size.height.value
                        )
                    )
                }
        }
    }
}


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    initialConfig: BinaryClockWidgetConfig = BinaryClockWidgetConfig(),
    onConfirm: (BinaryClockWidgetConfig) -> Unit,
) {
    var showBitLabels by remember { mutableStateOf(initialConfig.showBitLabels) }
    var showHmsLabels by remember { mutableStateOf(initialConfig.showHmsLabels) }
    var showSeconds by remember { mutableStateOf(initialConfig.showSeconds) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showSeconds) {
        while (true) {
            val now = System.currentTimeMillis()
            val interval = if (showSeconds) 1000L else 60_000L
            val next = now - (now % interval) + interval
            kotlinx.coroutines.delay(next - now)
            tick = System.currentTimeMillis()
        }
    }

    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val triggerRecomposition = tick
    val now = LocalTime.now()
    val digits = if (showSeconds) {
        BinaryClockDigits.timeDigits(now.hour, now.minute, now.second)
    } else {
        BinaryClockDigits.timeDigits(now.hour, now.minute)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Configure Widget",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Preview
        @Suppress("UnusedBoxWithConstraintsScope")
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val previewWidth = minOf(maxWidth, 300.dp)
            val previewHeight = previewWidth * 0.55f
            val size = DpSize(previewWidth, previewHeight)

            val remoteViews by produceState<android.widget.RemoteViews?>(
                initialValue = null, digits, size, showBitLabels, showHmsLabels, showSeconds,
            ) {
                value = GlanceRemoteViews().compose(
                    context = context,
                    size = size,
                ) {
                    BinaryClockWidgetContent(
                        digits = digits,
                        showBitLabels = showBitLabels,
                        showHmsLabels = showHmsLabels,
                    )
                }.remoteViews
            }

            remoteViews?.let { rv ->
                AndroidView(
                    modifier = Modifier
                        .width(previewWidth)
                        .height(previewHeight),
                    factory = { ctx ->
                        val density = ctx.resources.displayMetrics.density
                        val widthPx = (previewWidth.value * density).toInt()
                        val heightPx = (previewHeight.value * density).toInt()
                        FrameLayout(ctx).apply {
                            val child = rv.apply(ctx, this)
                            child.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
                            addView(child)
                        }
                    },
                    update = { frameLayout ->
                        val density = frameLayout.context.resources.displayMetrics.density
                        val widthPx = (previewWidth.value * density).toInt()
                        val heightPx = (previewHeight.value * density).toInt()
                        frameLayout.removeAllViews()
                        val child = rv.apply(frameLayout.context, frameLayout)
                        child.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
                        frameLayout.addView(child)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConfigToggle(
            label = "Show seconds",
            checked = showSeconds,
            onCheckedChange = { showSeconds = it },
        )
        ConfigToggle(
            label = "Show bit labels",
            checked = showBitLabels,
            onCheckedChange = { showBitLabels = it },
        )
        ConfigToggle(
            label = "Show hint",
            checked = showHmsLabels,
            onCheckedChange = { showHmsLabels = it },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onConfirm(BinaryClockWidgetConfig(
                    showBitLabels = showBitLabels,
                    showHmsLabels = showHmsLabels,
                    showSeconds = showSeconds,
                ))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun ConfigToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BinaryClockWidgetTheme {
        MainScreen(
            uiState = MainViewState(providers = listOf(
                ProviderData(
                    provider = BinaryClockGlanceWidget::class.java,
                    receiver = BinaryClockWidgetReceiver::class.java,
                    appWidgets = listOf(
                        AppWidgetDesc(
                            appWidgetId = AppWidgetId(1),
                            sizes = listOf(DpSize.Zero)
                        ),
                        AppWidgetDesc(
                            appWidgetId = AppWidgetId(2),
                            sizes = listOf(DpSize.Zero)
                        )
                    ),
                )
            )),
            onProviderClicked = {}
        )
    }
}