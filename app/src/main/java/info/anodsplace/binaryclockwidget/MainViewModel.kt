package info.anodsplace.binaryclockwidget

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AppWidgetId(val appWidgetId: Int) : GlanceId

data class ProviderData(
    val provider: Class<out GlanceAppWidget>,
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val appWidgets: List<AppWidgetDesc>,
)

data class AppWidgetDesc(
    val appWidgetId: GlanceId,
    val sizes: List<DpSize>,
)

data class MainViewState(
    val providers: List<ProviderData> = emptyList()
)

class MainViewModel(
    val appWidgetId: Int,
    private val app: Application,
    private val appWidgetManager: AppWidgetManager,
    private val glanceWidgetManger: GlanceAppWidgetManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainViewState())
    val uiState: StateFlow<MainViewState> = _uiState.asStateFlow()

    class Factory(private val appWidgetId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            // Get the Application object from extras
            val application = checkNotNull(extras[APPLICATION_KEY])
            // Create a SavedStateHandle for this ViewModel from extras
            val savedStateHandle = extras.createSavedStateHandle()

            return MainViewModel(
                appWidgetId = appWidgetId,
                app = application,
                appWidgetManager = AppWidgetManager.getInstance(application),
                glanceWidgetManger = GlanceAppWidgetManager(application),
                savedStateHandle = savedStateHandle
            ) as T
        }
    }

    private val receivers: List<String>
        get() =  appWidgetManager.installedProviders
            .filter { it.provider.packageName == app.packageName }
            .map { it.provider.className }

    fun update() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    providers = collectProviders()
                )
            }
        }
    }

    fun onProviderClicked(providerData: ProviderData) {
        viewModelScope.launch {
            glanceWidgetManger.requestPinGlanceAppWidget(
                receiver = providerData.receiver,
                preview = providerData.provider.getDeclaredConstructor()
                    .newInstance(),
                previewState = emptyPreferences()
            )
        }
    }

    private suspend fun collectProviders(): List<ProviderData> {
        // Discover the GlanceAppWidget
        return receivers.mapNotNull { receiverName ->
            val receiverClass = Class.forName(receiverName)
            if (!GlanceAppWidgetReceiver::class.java.isAssignableFrom(receiverClass)) {
                return@mapNotNull null
            }
            val receiver = receiverClass.getDeclaredConstructor()
                .newInstance() as GlanceAppWidgetReceiver
            val provider = receiver.glanceAppWidget.javaClass
            ProviderData(
                provider = provider,
                receiver = receiver.javaClass,
                appWidgets = glanceWidgetManger.getGlanceIds(provider).map { id ->
                    AppWidgetDesc(appWidgetId = id, sizes = glanceWidgetManger.getAppWidgetSizes(id))
                })
        }
    }
}