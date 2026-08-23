package com.invisusnova.privadot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val CAMERA_DOT_COLOR = stringPreferencesKey("camera_dot_color")
        val MIC_DOT_COLOR = stringPreferencesKey("mic_dot_color")
        val LOCATION_DOT_COLOR = stringPreferencesKey("location_dot_color")
        val DOT_SIZE = intPreferencesKey("dot_size")
        
        val CAMERA_POS_X = floatPreferencesKey("camera_pos_x")
        val CAMERA_POS_Y = floatPreferencesKey("camera_pos_y")
        val MIC_POS_X = floatPreferencesKey("mic_pos_x")
        val MIC_POS_Y = floatPreferencesKey("mic_pos_y")
        val LOCATION_POS_X = floatPreferencesKey("location_pos_x")
        val LOCATION_POS_Y = floatPreferencesKey("location_pos_y")
    }

    val cameraColorFlow: Flow<String> = context.dataStore.data.map { it[CAMERA_DOT_COLOR] ?: "#3FB950" }
    val micColorFlow: Flow<String> = context.dataStore.data.map { it[MIC_DOT_COLOR] ?: "#F0883E" }
    val locationColorFlow: Flow<String> = context.dataStore.data.map { it[LOCATION_DOT_COLOR] ?: "#58A6FF" }

    // Default size is 8dp
    val dotSizeFlow: Flow<Int> = context.dataStore.data.map { it[DOT_SIZE] ?: 8 }

    // Defaults spaced out horizontally near Top-Right, avoiding notches (Y=0.03f)
    val cameraPosXFlow: Flow<Float> = context.dataStore.data.map { it[CAMERA_POS_X] ?: 0.86f }
    val cameraPosYFlow: Flow<Float> = context.dataStore.data.map { it[CAMERA_POS_Y] ?: 0.03f }
    
    val micPosXFlow: Flow<Float> = context.dataStore.data.map { it[MIC_POS_X] ?: 0.93f }
    val micPosYFlow: Flow<Float> = context.dataStore.data.map { it[MIC_POS_Y] ?: 0.03f }
    
    val locationPosXFlow: Flow<Float> = context.dataStore.data.map { it[LOCATION_POS_X] ?: 1.0f }
    val locationPosYFlow: Flow<Float> = context.dataStore.data.map { it[LOCATION_POS_Y] ?: 0.03f }

    suspend fun saveCameraColor(hex: String) { context.dataStore.edit { it[CAMERA_DOT_COLOR] = hex } }
    suspend fun saveMicColor(hex: String) { context.dataStore.edit { it[MIC_DOT_COLOR] = hex } }
    suspend fun saveLocationColor(hex: String) { context.dataStore.edit { it[LOCATION_DOT_COLOR] = hex } }
    suspend fun saveDotSize(size: Int) { context.dataStore.edit { it[DOT_SIZE] = size } }

    suspend fun saveCameraPos(x: Float, y: Float) { context.dataStore.edit { it[CAMERA_POS_X] = x; it[CAMERA_POS_Y] = y } }
    suspend fun saveMicPos(x: Float, y: Float) { context.dataStore.edit { it[MIC_POS_X] = x; it[MIC_POS_Y] = y } }
    suspend fun saveLocationPos(x: Float, y: Float) { context.dataStore.edit { it[LOCATION_POS_X] = x; it[LOCATION_POS_Y] = y } }
    
    suspend fun resetToDefaults() {
        context.dataStore.edit {
            it.clear()
        }
    }
}
