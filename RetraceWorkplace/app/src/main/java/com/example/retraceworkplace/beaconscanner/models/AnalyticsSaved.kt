package com.example.retraceworkplace.beaconscanner.models

import android.text.format.DateUtils
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import com.example.retraceworkplace.beaconscanner.Database.AnalyticsDao
import com.example.retraceworkplace.beaconscanner.Database.BeaconsDao
//import com.example.retraceworkplace.beaconscanner.utils.BuildTypes
//import com.example.retraceworkplace.beaconscanner.utils.RuuviParser
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor
import timber.log.Timber
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by abhipsa_k on 04/07/2022.
 */

@Entity(
        tableName = AnalyticsDao.TABLE_NAME,
    primaryKeys = ["day","month","year"]
)
data class AnalyticsSaved(
    @SerializedName("hashcode")
    @ColumnInfo(name = "hashcode")
    val hashcode: Int = 0, // hashcode()

    @SerializedName("day")
    @ColumnInfo(name = "day")
    val day: Int = 0, // analytics date

    @SerializedName("month")
    @ColumnInfo(name = "month")
    val month:  Int = 0, // analytics date

    @SerializedName("year")
    @ColumnInfo(name = "year")
    val year:  Int = 0, // analytics date

    @SerializedName("steps")
    @ColumnInfo(name = "steps")
    val steps: Int = 0, // analytics date

    @SerializedName("meeting_time")
    @ColumnInfo(name = "meeting_time")
    val meeting_time: Float = 0F,

    @SerializedName("lunch_time")
    @ColumnInfo(name = "lunch_time")
    val lunch_time: Float = 0F,

    @SerializedName("total_time")
    @ColumnInfo(name = "total_time")
    val total_time: Float = 0F,

    @SerializedName("walking_time")
    @ColumnInfo(name = "walking_time")
    val walking_time: Float = 0F,

    @SerializedName("socialise_time")
    @ColumnInfo(name = "socialise_time")
    val socialise_time: Float = 0F,

    @SerializedName("work_time")
    @ColumnInfo(name = "work_time")
    val work_time: Float = 0F,


) {
    companion object {

        fun saveAnalyticData(totTime: Float, walkingTime:Float,lunchTime:Float, meetingTime:Float, socialiseTime:Float, workTime:Float, steps: Int) : AnalyticsSaved {
            Log.d("ActivityUpdate","saving analytic data");

            val sdf = SimpleDateFormat("dd-mm-yyyy")
            val currentDate: String = sdf.format(Date())
            var hashcode = currentDate.hashCode()
            Log.d("ActivityUpdate","${Date().getDate()}/${Date().month}/${Date().year}/$hashcode");
            return AnalyticsSaved(
                    hashcode = hashcode,
                    day = Date().getDate(),
                    month = Date().month,
                    year = Date().year,
                    total_time = (walkingTime+lunchTime+meetingTime+socialiseTime+workTime),
                    walking_time = walkingTime,
                    lunch_time = lunchTime,
                    meeting_time = meetingTime,
                    socialise_time = socialiseTime,
                    work_time = workTime,
                    steps = steps
            )
        }
    }

    fun toJson(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }
}
