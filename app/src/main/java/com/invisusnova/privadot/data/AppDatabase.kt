package com.invisusnova.privadot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SupportFactory

@Database(entities = [HistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    suspend fun secureClearAndVacuum() {
        withContext(Dispatchers.IO) {
            try {
                openHelper.writableDatabase.execSQL("PRAGMA secure_delete = ON;")
                historyDao().clearHistory()
                openHelper.writableDatabase.execSQL("VACUUM;")
            } catch (e: Exception) {
                e.printStackTrace()
                historyDao().clearHistory()
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = DatabaseKeyManager.getPassphrase(context.applicationContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privadot_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
