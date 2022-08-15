package com.example.retraceworkplace.beaconscanner.models

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.retraceworkplace.MainActivity
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import javax.inject.Inject

class BluetoothManager @Inject constructor(private val adapter: BluetoothAdapter?, context: Context) {

    private val subject: BehaviorProcessor<MainActivity.BluetoothState> =
            BehaviorProcessor.createDefault<MainActivity.BluetoothState>(getStaeFromAdapterState(adapter?.state ?: BluetoothAdapter.STATE_OFF))

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                    val state = getStaeFromAdapterState(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR))

                    subject.onNext(state)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun getStaeFromAdapterState(state: Int) : MainActivity.BluetoothState {
        return when (state) {
            BluetoothAdapter.STATE_OFF -> MainActivity.BluetoothState.STATE_OFF
            BluetoothAdapter.STATE_TURNING_OFF -> MainActivity.BluetoothState.STATE_TURNING_OFF
            BluetoothAdapter.STATE_ON -> MainActivity.BluetoothState.STATE_ON
            BluetoothAdapter.STATE_TURNING_ON -> MainActivity.BluetoothState.STATE_TURNING_ON
            else -> MainActivity.BluetoothState.STATE_OFF
        }
    }

    fun disable() = adapter?.disable()

    fun enable() = adapter?.enable()

    fun asFlowable(): Flowable<MainActivity.BluetoothState> {
        return subject
    }

    fun isEnabled() = adapter?.isEnabled == true

    fun toggle() {
        if (isEnabled()) {
            disable()
        } else {
            enable()
        }
    }
}
