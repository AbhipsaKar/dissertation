package com.example.retraceworkplace

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.app.ActivityCompat

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.retraceworkplace.beaconscanner.Database.AnalyticsDao
import com.example.retraceworkplace.beaconscanner.Database.AppDatabase
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import com.example.retraceworkplace.beaconscanner.models.BluetoothManager
import com.example.retraceworkplace.beaconscanner.models.GEOFENCE_BEACON_ID
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarMenu
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), MonitorNotifier, BeaconConsumer{
    private var beaconManager: BeaconManager? = null
    private var startBtn: Button? = null /* Start tracking button */
    private var startBack:ImageView? = null
    private var homeBack:ImageView? = null
    private var msg:TextView? = null
    private var bottomNav:BottomNavigationView? = null
    private var bluetoothadapter: BluetoothAdapter? = null
    private var region1 = Region("com.example.retraceworkplace", Identifier.parse(GEOFENCE_BEACON_ID),null, null)

    private lateinit var viewModel: HomeActivityViewModel
    private lateinit var viewModelFactory: HomeActivityViewModelFactory

    lateinit var bluetoothState: BluetoothManager
    private var bluetoothStateDisposable: Disposable? = null
    private var avgSteps:TextView? = null
    private var avgTime:TextView? = null

    @Inject
    lateinit var db: AnalyticsDao

    enum class RegionState
    {
        STATE_INSIDE,
        STATE_OUTSIDE,
    }


    enum class BluetoothState(
        @ColorRes val bgColor: Int,
        @StringRes val text: Int
    ) {
        STATE_OFF(R.color.bluetoothDisabled, R.string.bluetooth_disabled),
        STATE_TURNING_OFF(R.color.bluetoothTurningOff, R.string.turning_bluetooth_off),
        STATE_ON(R.color.bluetoothTurningOn, R.string.bluetooth_enabled),
        STATE_TURNING_ON(R.color.bluetoothTurningOn, R.string.turning_bluetooth_on)
    }

    /* Update geofence state on view */
    private fun updateMessage(state:RegionState){
        if(state == RegionState.STATE_OUTSIDE)
            msg?.text = getString(R.string.Outside)
        else
            msg?.text = getString(R.string.Inside)


    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun observeBluetoothState() {
        // Setup an observable on the bluetooth changes
        bluetoothStateDisposable?.dispose()
        bluetoothStateDisposable = bluetoothState.asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { newState ->
                //updateBluetoothState(newState, bluetoothState.isEnabled())
                showBluetoothStatus(newState)
                if (newState == BluetoothState.STATE_OFF) {
                    stopScan()
                }
                else if(newState == BluetoothState.STATE_ON){
                    startScan()
                }
            }
    }

    private fun stopScan()
    {
        if (beaconManager?.isBound(this) == true) {
            Timber.d("Unbinding from beaconManager")
            beaconManager?.unbind(this)
        }
    }

    /* view model observes geofencing state view component */
    private fun observeViewModel( viewModel: HomeActivityViewModel) {
        viewModel.getOfficeState().observe(this, androidx.lifecycle.Observer {

            Log.d("ActivityUpdate", "Live data on changed $it")
            if (it != null) {
                updateMessage(it)
                //plotxy(it)

            }

        });
        viewModel.totTime.observe(this, androidx.lifecycle.Observer {

            Log.d("ActivityUpdate", "Live data on changed $it")
            if (it != null) {
                avgTime?.setText(String.format("%.02f secs", it))

            }

        });
        viewModel.avgSteps.observe(this, androidx.lifecycle.Observer {

            Log.d("ActivityUpdate", "Live data on changed $it")
            if (it != null) {
                avgSteps?.setText(String.format("%.02f", it))

            }

        });

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startScan(){
        if ((bluetoothadapter?.isEnabled == false) ) {
            return showBluetoothStatus(BluetoothState.STATE_OFF)
        }
        checkPermissions(this, this)
            .also{
                if (beaconManager?.isBound(this) != true) {
                    Log.d("ActivityUpdate","binding beaconManager")
                    beaconManager?.bind(this)
                }
            }


    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home)
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav?.setOnNavigationItemSelectedListener(navListener)
        bottomNav?.setSelectedItemId(R.id.Home);

        db = AppDatabase.getDatabase(application).analyticsDao()
        viewModelFactory = HomeActivityViewModelFactory()
        viewModel = ViewModelProvider(this,viewModelFactory).get(HomeActivityViewModel::class.java)
        bluetoothState = BluetoothManager(BluetoothAdapter.getDefaultAdapter(), this)
        observeViewModel(viewModel);
        observeBluetoothState()

        initialise()
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout(EDDYSTONE_UID));
        beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout(EDDYSTONE_URL));

        beaconManager?.foregroundScanPeriod = 600L
        beaconManager?.foregroundBetweenScanPeriod = 2000L

        startScan()

        /* Set on click behavior for start tracking button */
        startBtn?.setOnClickListener {
            Tracking()
            val intent = Intent (this.applicationContext, DetectActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }



        /* Update average office time and steps */
        GlobalScope.launch {
            repeat(1) {
                Log.d("ActivityUpdate", "Hello Main")
                viewModel.avgSteps.postValue(db.getAvgSteps())
                viewModel.totTime.postValue(db.getAvgTime())
                //Log.d("ActivityUpdate", "print today's data")

            }
            //delay(100000) // Suspends the coroutine for some time
        }


    }

    fun checkPermissions(activity: Activity?, context: Context?) {
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
        if (!hasPermissions(context, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity!!, PERMISSIONS, PERMISSION_ALL)
        }
        else
        {
            Log.d("ActivityUpdate","Permission granted")
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch {
            repeat(1) {
                Log.d("ActivityUpdate", "Hello Main")
                viewModel.avgSteps.postValue(db.getAvgSteps())
                viewModel.totTime.postValue(db.getAvgTime())


            }

        }
    }

    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    private fun showBluetoothStatus(bluestate:BluetoothState){
        var viewPos:View  = findViewById(R.id.myCoordinatorLayout);
        if(bluestate == BluetoothState.STATE_OFF){
            Snackbar.make(viewPos, getString(bluestate.text), Snackbar.LENGTH_INDEFINITE )
                .setBackgroundTint(getColor(bluestate.bgColor))
                .show()
        }
        else{
            Snackbar.make(viewPos, getString(bluestate.text), Snackbar.LENGTH_LONG )
                .setBackgroundTint(getColor(bluestate.bgColor))
                .show()
        }


    }
    /* Start beacon monitoring */
    override fun onBeaconServiceConnect() {
        Log.d("ActivityUpdate","beaconManager is bound, ready to start scanning")
        beaconManager?.addMonitorNotifier(this)

        try {
            Log.d("ActivityUpdate","Start monitoring region")
            beaconManager?.startMonitoringBeaconsInRegion(region1)



        } catch (e: RemoteException) {
            Log.d("ActivityUpdate","Error ranging beacons")
            e.printStackTrace()
        }
    }
    @SuppressLint("ResourceType")
    private fun showButton()
    {
        startBtn?.visibility = View.VISIBLE
        startBtn?.startAnimation(AnimationUtils.loadAnimation(this, R.animator.button_view));
        bottomNav?.menu?.getItem(1)?.isEnabled = true
    }

    @SuppressLint("ResourceType")
    private fun hideButton()
    {
        startBtn?.visibility = View.GONE
        startBtn?.startAnimation(AnimationUtils.loadAnimation(this, R.animator.button_hide));
        bottomNav?.menu?.getItem(1)?.isEnabled = false
    }

    /* Update geofence state when beacon observer is called */
    override fun didDetermineStateForRegion(p0: Int, p1: Region?) {
        Log.d("ActivityUpdate","didDetermineStateForRegion");
        Log.d("ActivityUpdate", "${p0.toString()} and ${p1.toString()}");

        if(p0 == 1){
            viewModel.getOfficeState().postValue(RegionState.STATE_INSIDE)
            showButton()

        }
        else {
            viewModel.getOfficeState().postValue(RegionState.STATE_OUTSIDE)
            hideButton()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager!!.unbind(this)
    }

    override fun didEnterRegion(p0: Region?) {
        Log.d("ActivityUpdate","didEnterRegion");
        viewModel.getOfficeState().postValue(RegionState.STATE_INSIDE)
        showButton()
    }

    override fun didExitRegion(p0: Region?) {
        Log.d("ActivityUpdate","didExitRegion");

        viewModel.getOfficeState().postValue(RegionState.STATE_OUTSIDE)
        hideButton()
    }

    /*  Initialise view components */
    private fun initialise(){
        startBtn =  findViewById(R.id.start_btn);
        startBack = findViewById(R.id.home_start_back)
        homeBack = findViewById(R.id.home_back)
        avgTime = findViewById(R.id.avgTime)
        avgSteps =findViewById(R.id.avgSteps)
        msg = findViewById(R.id.message)
        msg?.text = getString(R.string.Outside)
        startBtn?.setVisibility(View.GONE)
        startBack?.setVisibility(View.GONE)


    }

    /* Bottom navigation bar */
    private val navListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item -> // By using switch we can easily get
            // the selected fragment
            // by using there id.
            var selectedFragment: Fragment? = null
            when (item.itemId) {
               // R.id.Home -> selectedFragment = Home()
                R.id.Tracking -> {
                    Tracking()
                    val intent = Intent (this.applicationContext, DetectActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent)
                }
                R.id.History -> {
                    History()
                    val intent = Intent (this.applicationContext, AnalyticActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent)
                }
            }

            true
        }

}

