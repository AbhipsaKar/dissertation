package com.example.retraceworkplace.beaconscanner.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface AnalyticsDao {

    companion object {
        const val TABLE_NAME = "analytics"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE month = :month AND year = :year")
    fun getAnalyticsForMonth(month:Int, year:Int) : List<AnalyticsSaved>

    @Query("SELECT * FROM $TABLE_NAME WHERE day = :day AND month = :month AND year = :year")
    fun getAnalyticsForDay(day:Int,month:Int, year:Int) : AnalyticsSaved

    @Query("SELECT AVG(steps) FROM $TABLE_NAME")
    fun getAvgSteps() : Float

    @Query("SELECT AVG(total_time) FROM $TABLE_NAME")
    fun getAvgTime() : Float

    @Insert(onConflict = REPLACE)
    fun insertAnalytics(analyticsSaved: AnalyticsSaved)

    @Delete
    fun deleteAnalytics(analyticsSaved: AnalyticsSaved)

    @Query("DELETE FROM $TABLE_NAME")
    fun clearAnalytics()
}