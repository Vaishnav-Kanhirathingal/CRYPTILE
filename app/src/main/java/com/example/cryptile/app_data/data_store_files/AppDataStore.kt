package com.example.cryptile.app_data.data_store_files

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_data_store"
)

class AppDataStore(val context: Context) {
    //------------------------------------------------------------------------------------------keys
    private val fingerprintAppLockKey = booleanPreferencesKey("fingerprint_app_lock_key")
    private val keepMeSignedInKey = booleanPreferencesKey("keep_me_signed_in_key")

    //------------------------------------------------------------------------------------value-flow

    val fingerprintAppLockFlow: Flow<Boolean> = booleanMapper(fingerprintAppLockKey)
    val keepMeSignedInFlow: Flow<Boolean> = booleanMapper(keepMeSignedInKey)

    //----------------------------------------------------------------------------------------mapper

    /**
     * boolean mapper is used to access every boolean stored in the data store.
     */
    private fun booleanMapper(key: Preferences.Key<Boolean>): Flow<Boolean> {
        return context.dataStore.data.catch { it.printStackTrace() }.map { it[key] ?: false }
    }

    //---------------------------------------------------------------------------------------setters

    /**
     * savers are used to save the values they get as parameters to the datastore. boolean saver is
     * used to store boolean values. storeBoolean is an enum which is used to decide, under what
     * key name the value is to be stored.
     */
    suspend fun booleanSaver(boolean: Boolean, storeBoolean: StoreBoolean) {
        context.dataStore.edit {
            it[when (storeBoolean) {
                StoreBoolean.USER_USES_FINGERPRINT -> fingerprintAppLockKey
                StoreBoolean.KEEP_ME_SIGNED_IN -> keepMeSignedInKey
                else -> {
                    throw IllegalArgumentException("boolean type not specified in data store")
                }
            }] = boolean
        }
    }
}

//---------------------------------------------------------------------------------------------enums

/**
 * these enums are used by saver functions to decide where the provided value is to be stored
 */
enum class StoreBoolean {
    USER_USES_FINGERPRINT,
    KEEP_ME_SIGNED_IN
}