package com.example.retraceworkplace.beaconscanner.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import com.example.retraceworkplace.beaconscanner.models.BeaconSaved
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database( version = 5, entities = [BeaconSaved::class,AnalyticsSaved::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun beaconsDao() : BeaconsDao
    abstract fun analyticsDao() : AnalyticsDao

    companion object {
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "room_db"

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(BeaconSaved::class.java) {
                    synchronized(AnalyticsSaved::class.java){
                        if (INSTANCE == null) {
                            INSTANCE = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                                //.allowMainThreadQueries() // Uncomment if you don't want to use RxJava or coroutines just yet (blocks UI thread)
                                .addCallback(object : Callback() {
                                    override fun onCreate(db: SupportSQLiteDatabase) {
                                        super.onCreate(db)
                                        Log.d("AppDatabase", "populating with data...")
                                        GlobalScope.launch(Dispatchers.IO) {  }
                                    }
                                })
                                .fallbackToDestructiveMigration()
                                .build()
                        }
                    }

                }
            }

            return INSTANCE!!
        }
    }
}