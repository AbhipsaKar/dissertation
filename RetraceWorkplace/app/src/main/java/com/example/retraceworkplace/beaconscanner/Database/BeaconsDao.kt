package com.example.retraceworkplace.beaconscanner.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.example.retraceworkplace.beaconscanner.models.BeaconSaved
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface BeaconsDao {

    companion object {
        const val TABLE_NAME = "beacons"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE is_blocked = :blocked")
    fun getBeacons(blocked: Boolean = false) : List<BeaconSaved>

    @Query("SELECT * FROM $TABLE_NAME WHERE hashcode = :hashcode")
    fun getBeaconById(hashcode: Int) : Single<BeaconSaved>

    @Query("SELECT * FROM $TABLE_NAME WHERE eddystone_uid_data_namespace_id = :beaconAddress")
    fun getBeaconByAddress(beaconAddress: String) : Single<BeaconSaved>

    @Query("SELECT * FROM $TABLE_NAME where is_blocked = :blocked AND last_seen >= :lastSeen")
    fun getBeaconsSeenAfter(lastSeen: Long, blocked: Boolean = false) : Single<List<BeaconSaved>>

    @Query("SELECT MAX(last_seen) FROM $TABLE_NAME")
    fun getBeaconScanTime() : Long

    @Insert(onConflict = REPLACE)
    fun insertBeacon(beacon: BeaconSaved)

    @Delete
    fun deleteBeacon(beacon: BeaconSaved)

    @Query("DELETE FROM $TABLE_NAME WHERE hashcode = :hashcode")
    fun deleteBeaconById(hashcode: Int)

    @Query("DELETE FROM $TABLE_NAME")
    fun clearBeacons()
}