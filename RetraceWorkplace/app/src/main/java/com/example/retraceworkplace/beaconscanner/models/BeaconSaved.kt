package com.example.retraceworkplace.beaconscanner.models

import android.text.format.DateUtils
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import com.example.retraceworkplace.beaconscanner.Database.BeaconsDao
//import com.example.retraceworkplace.beaconscanner.utils.BuildTypes
//import com.example.retraceworkplace.beaconscanner.utils.RuuviParser
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.*

/**
 * Created by bridou_n on 30/09/2016.
 */

@Entity(
        tableName = BeaconsDao.TABLE_NAME,
        primaryKeys = [
            "hashcode"
        ]
)
data class BeaconSaved(
    @SerializedName("hashcode")
    @ColumnInfo(name = "hashcode")
    val hashcode: Int = 0, // hashcode()

    @SerializedName("beacon_type")
    @ColumnInfo(name = "beacon_type")
    val beaconType: String? = null, // Eddystone, altBeacon, iBeacon*/

    @SerializedName("beacon_address")
    @ColumnInfo(name = "beacon_address")
    val beaconAddress: String? = null, // MAC address of the bluetooth emitter

    @SerializedName("rssi")
    @ColumnInfo(name = "rssi")
    val rssi: Int = 0,

    @SerializedName("last_seen")
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long = 0,

    /**
     * Specialized field for every beacon type
     */


    @SerializedName("eddystone_url_data")
    @Embedded(prefix = "eddystone_url_data_")
    val eddystoneUrlData: EddystoneUrlData? = null,

    @SerializedName("eddystoneUidData")
    @Embedded(prefix = "eddystone_uid_data_")
    val eddystoneUidData: EddystoneUidData? = null,

    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean = false
) {
    companion object {
        const val TYPE_EDDYSTONE_UID = "eddystone_uid"
        const val TYPE_EDDYSTONE_URL = "eddystone_url"

        fun createFromBeacon(beacon: Beacon, isBlocked: Boolean = false) : BeaconSaved {
            Log.d("ActivityUpdate","createFromBeacon");

            // Common fields to every beacons
            var hashcode = beacon.hashCode()
            val lastSeen = Date().time
            var beaconAddress = beacon.bluetoothName
            val rssi = beacon.rssi
            var beaconType: String? = null
            var eddystoneUrlData: EddystoneUrlData? = null
            var eddystoneUidData: EddystoneUidData? = null



            beaconType = TYPE_EDDYSTONE_UID
            eddystoneUidData = EddystoneUidData(beacon.id1.toString(), beacon.id2.toString())

            Log.d("ActivityUpdate","This is a Eddystone-data frame");
            Timber.d(beacon.beaconTypeCode.toString());

            return BeaconSaved(
                    hashcode = hashcode,
                    beaconType = beaconType,
                    beaconAddress = beaconAddress,
                    rssi = rssi,
                    lastSeen = lastSeen,
                    eddystoneUrlData = eddystoneUrlData,
                    eddystoneUidData = eddystoneUidData,
                    isBlocked = isBlocked
            )
        }
    }

    fun toJson(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }
}
