package com.example.retraceworkplace.beaconscanner.models

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.retraceworkplace.R
import com.example.retraceworkplace.SupportedLocation
import org.altbeacon.beacon.Identifier
import java.util.ArrayList

const val GEOFENCE_BEACON_ID = "0x284769698cd28cdc4a1a" /* Set the id of the geofencing beacon */

enum class RoomLocation{
    LUNCH,
    LECTURE,
    MEETING_ROOM1,
    MEETING_ROOM2,
    COMMON
}

data class RoomBeaconTag(val m_roomName: RoomLocation, val m_beaconuidList:List<String>)

/* Set the list of beacon ids for every room */
var room = listOf(RoomBeaconTag(
    RoomLocation.LUNCH,listOf(
        "0x394a22f5f5707b41b2bd",
        "0x3d4549797f5b955a8078",
        "0x684c6dc5d42880381c30",
        "0x70494083dab304ec3708",
        "0xc04cb298f92c80de4a01",
        "0x144170f38880bd164c4c",
        "0xad47cf0d2dfd9188caf3",
        "0xee410560dca0282c6c2a"
        )
    ),
    RoomBeaconTag(RoomLocation.LECTURE,listOf(
        "0xf147926f0e9c2b125880",
        "0x1544348da864b399c299",
        "0x5d431c76fbe82e057180",
        "0x6345ef118017c6674e2d",
        "0x1f4fc890efa68680a646",
        "0x0e4006d7ccac60de37a3",
        "0xc04d32d1250b81127503",
        "0x4946d7ab47d12a7d0724",
        "0xe246e8fb16f3cf57db8b",
        "0xab4eeb721bd51949b81a"
        )
    ),
    RoomBeaconTag(RoomLocation.MEETING_ROOM1, listOf(
        "0xcb4e96360e1053c29c73",
        "0x274743d6b21db3b20e21",
        "0xeb44cc464321d349993b",
        "0x724e07cae59f847b5d2f",

        )
    ),
    RoomBeaconTag(RoomLocation.MEETING_ROOM2,listOf(
        "0x4948d2ec2dd8b41746a0",
        "0x6d4b285ded79b513f195",
        "0x4d4439703f4d51c11a7e",
        "0xa34b16be0bd657d02359",
        "0xcb4c7180d0a4ea24075a",
        "0x1140f53bcbc34dba004e",
        "0x6342c89b232bf2c049a4",
        "0xdf4feac157aaf8bc3261"
        )
    ),
    RoomBeaconTag(RoomLocation.COMMON, listOf(
        "0x6545cd81d5b194beec0c",
        "0x5a43117cc2b8c1df93cc",
        "0x1543c186b5b4d3480a6c",
        "0x48432935a0e7247e915d",
        "0x8f4614e88c08b5fc8793",
        "0x4048455e18e74488ee9d",
        "0x2b4a63d33d92fd8b64a9",
        "0x72434c7333a30d4644de"
        )
    )
)



