package info.anodsplace.binaryclockwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import info.anodsplace.binaryclockwidget.ui.theme.BinaryClockWidgetTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(appWidgetId = intent?.
            extras?.
            getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, viewModel.appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        setContent {
            BinaryClockWidgetTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val uiState by viewModel.uiState.collectAsState()
                    MainScreen(
                        uiState = uiState,
                        onProviderClicked = viewModel::onProviderClicked
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }
}

@Composable
fun MainScreen(uiState: MainViewState, onProviderClicked: (ProviderData) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Installed App Widgets",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.providers) {
                ShowProvider(it, onProviderClicked = onProviderClicked)
                HorizontalDivider(color = Color.Black)
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


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BinaryClockWidgetTheme {
        MainScreen(
            uiState = MainViewState(providers = listOf(
                ProviderData(
                    provider = GlanceAppWidget::class.java,
                    receiver = GlanceAppWidgetReceiver::class.java,
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