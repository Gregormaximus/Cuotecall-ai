package com.kuote.agent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.Quote

@Database(
    entities = [
        CompanyProfile::class,
        FieldService::class,
        Quote::class,
        CompanyWebConfig::class,
        Job::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KuoteDatabase : RoomDatabase() {

    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun serviceDao(): ServiceDao
    abstract fun quoteDao(): QuoteDao
    abstract fun webConfigDao(): WebConfigDao
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: KuoteDatabase? = null

        fun getDatabase(context: Context): KuoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KuoteDatabase::class.java,
                    "kuote_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
typealias CallCatchDatabase = KuoteDatabase
