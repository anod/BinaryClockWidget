package info.anodsplace.binaryclock

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

data class BinaryClockWidgetConfig(
    val showBitLabels: Boolean = true,
    val showHmsLabels: Boolean = true,
    val showSeconds: Boolean = false,
)

object BinaryClockWidgetConfigKeys {
    val showBitLabels = booleanPreferencesKey("show_bit_labels")
    val showHmsLabels = booleanPreferencesKey("show_hms_labels")
    val showSeconds = booleanPreferencesKey("show_seconds")
    val lastUpdated = longPreferencesKey("last_updated")

    fun fromPreferences(prefs: Preferences): BinaryClockWidgetConfig {
        return BinaryClockWidgetConfig(
            showBitLabels = prefs[showBitLabels] ?: true,
            showHmsLabels = prefs[showHmsLabels] ?: true,
            showSeconds = prefs[showSeconds] ?: false,
        )
    }
}
