package com.gymbro.mad.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymbro.mad.ui.theme.ThemeId
import com.gymbro.mad.ui.theme.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsStore by preferencesDataStore(name = "user_settings")

class UserPreferences(context: Context) {
    private val store = context.applicationContext.userSettingsStore
    private val themeKey = stringPreferencesKey("theme")
    private val weightUnitKey = stringPreferencesKey("weight_unit")

    val theme: Flow<ThemeId> = store.data.map { prefs ->
        runCatching { ThemeId.valueOf(prefs[themeKey].orEmpty()) }.getOrDefault(ThemeId.MOLTEN)
    }
    val weightUnit: Flow<WeightUnit> = store.data.map { prefs ->
        runCatching { WeightUnit.valueOf(prefs[weightUnitKey].orEmpty()) }.getOrDefault(WeightUnit.KG)
    }

    suspend fun setTheme(value: ThemeId) { store.edit { it[themeKey] = value.name } }
    suspend fun setWeightUnit(value: WeightUnit) { store.edit { it[weightUnitKey] = value.name } }
}
