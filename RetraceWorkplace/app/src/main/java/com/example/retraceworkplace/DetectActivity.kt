/*
 * Copyright (c) 2021 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 * 
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.example.retraceworkplace

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.RemoteException
import android.view.Menu
import com.afollestad.materialdialogs.MaterialDialog
import com.example.retraceworkplace.detectedactivity.DetectedActivityService
import com.example.retraceworkplace.transitions.TRANSITIONS_RECEIVER_ACTION
import com.example.retraceworkplace.transitions.TransitionsReceiver
import com.example.retraceworkplace.transitions.removeActivityTransitionUpdates
import com.example.retraceworkplace.transitions.requestActivityTransitionUpdates
import com.example.retraceworkplace.beaconscanner.models.BeaconSaved

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.altbeacon.beacon.*
import timber.log.Timber
import javax.inject.Inject
import android.bluetooth.BluetoothAdapter
import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import org.altbeacon.beacon.BeaconParser

import com.example.retraceworkplace.beaconscanner.Database.AppDatabase
import com.example.retraceworkplace.beaconscanner.Database.BeaconsDao
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch
import java.util.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.LineChartView
import com.example.retraceworkplace.beaconscanner.Database.AnalyticsDao
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import com.example.retraceworkplace.beaconscanner.models.GEOFENCE_BEACON_ID
import com.example.retraceworkplace.beaconscanner.models.room
import java.text.SimpleDateFormat


// Eddystone beacon advertisement layout
const val EDDYSTONE_UID = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"
const val EDDYSTONE_URL = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"


class DetectActivity: AppCompatActivity(), SensorEventListener, BeaconConsumer {

  private var  sensorManager:SensorManager? = null
  private var stepSensor: Sensor?  = null
  private var moveSensor: Sensor?  = null
  private var isStepSensorAvailable: Boolean  = false
  private var isMoveSensorAvailable: Boolean  = false

  /* View component variables */

  private var startBtn:Button? = null
  private var stopBtn:Button? = null
  private var btnReset:Button? = null
  private var spinner: ProgressBar? = null;
  private var activityImage:ImageView? = null
  private var activityTitle:TextView? = null
  private var locImage:ImageView? = null
  private var locTitle:TextView? = null
  private var linechart: LineChartView?= null

  /* Variables for calculating the acceleration */
  private var mGravity:List<Float>? = null
  private var mAccel:Double  =0.0
  private var mAccelCurrent:Double =0.0
  private var mAccelLast:Double =0.0
  private var stepCounter:Int =0
  private var totSteps:Int =0
  private var prevStepCounter:Int =0
  private var prevSteps:Int = 0
  private var hitCount:Int = 0
  private var hitSum:Double = 0.0
  private var hitResult:Double = 0.0
  private var SAMPLE_SIZE:Int = 50 // change this sample size as you want, higher is more precise but slow measure.
  private var THRESHOLD:Double = 0.3

  /* Y Axis values set to list of complex activities */
  private val yaxisData = arrayOf(
    "Lunch","Work","Transit","Meeting","Socialise"
  )
  //Initialise empty arrays
  val yAxisValues = mutableListOf<PointValue>()
  val axisValues = mutableListOf<AxisValue>()

  private val activityTime: MutableList<Float> = mutableListOf(0F,0F,0F,0F,0F)
  private val lines:MutableList<Line> = mutableListOf<Line>()


  /* Database objects */
  @Inject
  lateinit var db: BeaconsDao
  @Inject
  lateinit var db2: AnalyticsDao

  private var bluetoothadapter: BluetoothAdapter? = null
  private var beaconManager: BeaconManager? = null
  private var listQuery: Disposable? = null
  private var loggingRequests = CompositeDisposable()
  private var isScanning = false
  private var firstScanDone = false

  enum class OfficeActivity
  {
    STATE_LUNCH,
    STATE_WORK,
    STATE_EXERCISE,
    STATE_MEETING,
    STATE_SOCIALISE
  }

  private var simpleActivity: SupportedActivity? = null
  private lateinit var viewModel: CurrentActivityViewModel
  private lateinit var viewModelFactory: CurrentActivityViewModelFactory

  suspend fun fetchbeacons(): List<BeaconSaved> = db.getBeacons(blocked = false)

  suspend fun clearbeacons() = db.clearBeacons()

  private var lastSeen:Long =0 /* Time of last beacon scan */
  private var lastActivity:OfficeActivity? = null


  override public fun onAccuracyChanged(p0: Sensor?, p1: Int){
    Log.d("ActivityUpdate","Accuracy changed")
  }

  /* To show a spinner until beacon scanning has started */
  private var isTrackingStarted = false
    set(value) {
      btnReset?.visibility = if(value) View.VISIBLE else View.GONE
      field = value
    }


  private val transitionBroadcastReceiver: TransitionsReceiver = TransitionsReceiver().apply {
    action = { setDetectedActivity(it) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.hasExtra(SUPPORTED_ACTIVITY_KEY)) {
      val supportedActivity = intent.getSerializableExtra(
          SUPPORTED_ACTIVITY_KEY) as SupportedActivity
      setDetectedActivity(supportedActivity)
    }
  }

  /* Check permissions before starting to scan */
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

  /* Start beacon scanning operation */
  private fun startScan() {
    Log.d("ActivityUpdate","Abby: Start scanning");

    if ((bluetoothadapter?.isEnabled == false) ) {
      return showBluetoothNotEnabledError()
    }

    checkPermissions(this@DetectActivity, this);


    if(sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER ) != null)
    {
      sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
      //txtCheck.setText(txtCheck.getText() + System.getProperty("line.separator") + "listener registered!");
    }
    if(sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER ) != null)
    {
      sensorManager?.registerListener(this, moveSensor, SensorManager.SENSOR_DELAY_NORMAL);
      //txtCheck.setText(txtCheck.getText() + System.getProperty("line.separator") + "listener registered!");
    }



    if (beaconManager?.isBound(this) != true) {
      Log.d("ActivityUpdate","binding beaconManager")
      beaconManager?.bind(this)
    }
    enableStopBtn()
    disableStartBtn()

    isScanning = true
  }

  /* Stop beacon scanning operation */
  private fun stopScan() {

    Log.d("ActivityUpdate","Stop scan");
    if(sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER ) != null)
    {
      sensorManager?.unregisterListener(this, stepSensor);
    }
    if(sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER ) != null)
    {
      sensorManager?.unregisterListener(this, moveSensor);
    }

    unbindBeaconManager()
    disableStopBtn()
    enableStartBtn()

    isScanning = false
    firstScanDone = false
    listQuery?.dispose()
    loggingRequests.clear()


    //bluetoothStateDisposable?.dispose()
  }

  private fun showBluetoothNotEnabledError() {
    Toast.makeText(this, "Switch on the bluetooth",
      Toast.LENGTH_SHORT).show()

  }


  private fun toggleScan() {
    if (!isScanning()) {
      Log.d("ActivityUpdate","start_scanning_clicked")
      return startScan()
    }
    Log.d("ActivityUpdate", "stop_scanning_clicked")
    stopScan()
  }


  /* Bottom navigation bar operation */
  private val navListener =
    BottomNavigationView.OnNavigationItemSelectedListener { item -> // By using switch we can easily get
      // the selected fragment
      // by using there id.
      var selectedFragment: Fragment? = null
      when (item.itemId) {
        R.id.Home -> {
          Home()
          val intent = Intent (this.applicationContext, MainActivity::class.java)
          intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
          startActivity(intent)
        }
        //R.id.Tracking -> selectedFragment = true
        R.id.History -> {
          History()
          val intent = Intent (this.applicationContext, AnalyticActivity::class.java)
          intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
          startActivity(intent)
        }
      }

      true
    }

  /* Map view model to activity, location and line chart view components */
  private fun observeViewModel( viewModel: CurrentActivityViewModel) {
    viewModel.getLiveData().observe(this,  androidx.lifecycle.Observer {

        Log.d("ActivityUpdate","Live data on changed $it")
        if (it != null) {

          plotxy(it)


        }

    });

    viewModel.getLocData().observe(this,  androidx.lifecycle.Observer {

      Log.d("ActivityUpdate","Loc data on changed $it")
      if (it != null) {
        setDetectedLocation(it)
      }

    });

    viewModel.getActData().observe(this,  androidx.lifecycle.Observer {

      Log.d("ActivityUpdate","Act data on changed $it")
      if (it != null) {
        setDetectedActivity(it)
      }

    });
  }

  /* Save analytic data whenever activity is updated */
  suspend fun updateAnalytics(act:OfficeActivity, time:Long)
  {

    Log.d("ActivityUpdate", "updateAnalytics ${act.toString()}:$time")
    when (act) {
      OfficeActivity.STATE_LUNCH -> { activityTime[0] = activityTime[0] + Math.round((time/10000).toFloat()) }
      OfficeActivity.STATE_WORK -> { activityTime[1] = activityTime[1] + Math.round((time/10000).toFloat()) }
      OfficeActivity.STATE_EXERCISE -> { activityTime[2] = activityTime[2] + Math.round((time/10000).toFloat()) }
      OfficeActivity.STATE_MEETING -> { activityTime[3] = activityTime[3] + Math.round((time/10000).toFloat()) }
      OfficeActivity.STATE_SOCIALISE -> { activityTime[4] = activityTime[4] + Math.round((time/10000).toFloat()) }
    }
    Log.d("ActivityUpdate","time = $activityTime")
    AnalyticsSaved.saveAnalyticData(100F,activityTime[2],activityTime[0],activityTime[3],activityTime[4],activityTime[1],stepCounter)
      .also {
        Log.d("ActivityUpdate","insert analytics")
        db2.insertAnalytics(it)
      }
  }

  @InternalCoroutinesApi
  override fun onCreate(savedInstanceState: Bundle?) {


    var v1 = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.FILL_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )


    super.onCreate(savedInstanceState)
    setContentView(R.layout.fragment_tracking)
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
    bottomNav.setOnNavigationItemSelectedListener(navListener)
    bottomNav.setSelectedItemId(R.id.Tracking);
    // as soon as the application opens the first
    // fragment should be shown to the user
    // in this case it is algorithm fragment


    initialize(); /* Initialise view components */

    viewModelFactory = CurrentActivityViewModelFactory()
    viewModel = ViewModelProvider(this,viewModelFactory).get(CurrentActivityViewModel::class.java)

    observeViewModel(viewModel);


    // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.

    db = AppDatabase.getDatabase(application).beaconsDao()
    db2 = AppDatabase.getDatabase(application).analyticsDao()
    beaconManager = BeaconManager.getInstanceForApplication(this);
    beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout(EDDYSTONE_UID));
    beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout(EDDYSTONE_URL));
    bluetoothadapter = BluetoothAdapter.getDefaultAdapter();


    /* Get the required sensor observers */
    if(sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER ) != null)
    {
      moveSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
      Log.d("ActivityUpdate", "Abby Sensor was found, move sensor is now ${moveSensor?.getName()}")
      isMoveSensorAvailable = true;
      mAccel = 0.00;
      mAccelCurrent = SensorManager.GRAVITY_EARTH.toDouble();
      mAccelLast = SensorManager.GRAVITY_EARTH.toDouble();
    }
    else
    {

      Log.d("ActivityUpdate", "Sensor was not found")
      isMoveSensorAvailable = false;
    }

    if(sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER ) != null)
    {
      stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER );
      Log.d("ActivityUpdate", "Sensor was found, stepsensor is now ${stepSensor?.getName()}")
      isStepSensorAvailable = true;
      stepCounter = 0 ;
      prevStepCounter = 0;
    }
    else
    {
      Log.d("ActivityUpdate", "Sensor was not found")
      isStepSensorAvailable = false;
    }

   GlobalScope.launch {
     while(true) {
       Log.d("ActivityUpdate", "Hello coroutine")
       if(isScanning && firstScanDone)
       {
         try {
           Log.d("ActivityUpdate", "Hello coroutine 1")
               val usersFromDb = fetchbeacons()
                 .filter{it.lastSeen > (Date().time - 40000)}
                 .sortedByDescending { it.rssi }
                 .also{

                   var room = lastActivity
                   if(it.size <=5)
                     room = identifyRoom(it)
                   else
                     room = identifyRoom(it.subList(0, 5))

                   viewModel.currentActivity.postValue(room)
                     .also{
                       if(lastActivity == null)
                       {
                         Log.d("ActivityUpdate", "analystics started")
                         lastActivity = room
                         lastSeen = Date().time
                       }
                       else if(lastActivity != room){
                         updateAnalytics(lastActivity!!, (Date().time - lastSeen) )
                         lastSeen = Date().time
                         lastActivity = room
                         Log.d("ActivityUpdate", "analystics room changed")
                       }
                     }

                 }
               Log.d("ActivityUpdate", "beacon scanning done")
               Log.d("ActivityUpdate", usersFromDb.toString())

         } catch (e: Exception) {
           // handler error
         }

       }

       //Log.d("ActivityUpdate", latestbeacons.toString())
       delay(20000) // Suspends the coroutine for some time

     }
    }

    startBtn?.setOnClickListener {
      if (isPermissionGranted()) {
        //startService(Intent(this, DetectedActivityService::class.java))
        //requestActivityTransitionUpdates()
        //showBluetoothNotEnabledError()
          Log.d("ActivityUpdate","Permission available. start scanning and tracking");
        toggleScan()
        isTrackingStarted = true
        spinner?.setVisibility(View.VISIBLE);

        Toast.makeText(this, "You've started activity tracking",
            Toast.LENGTH_SHORT).show()
      } else {
        Log.d("ActivityUpdate","Permission not available. Request first");
        requestPermission()
      }
    }
    stopBtn?.setOnClickListener {
      //stopService(Intent(this, DetectedActivityService::class.java))
      //removeActivityTransitionUpdates()
      toggleScan()
      Toast.makeText(this, "You've stopped tracking your activity", Toast.LENGTH_SHORT).show()
      spinner?.setVisibility(View.GONE);

    }
    btnReset?.setOnClickListener {
      resetTracking()
    }


    /* Indoor geolocation */
    //toggleScan()


  }

  private fun identifyRoom(currentList:List<BeaconSaved>) :OfficeActivity {
    var count_room: MutableList<Int> = mutableListOf(0,0,0,0,0)


    //var list:List<BeaconRow.Beacon> = currentList as kotlin.collections.List<BeaconRow.Beacon>
    Log.d("ActivityUpdate","Itemcount = ${currentList.size}")
    var closestBeacon:Int? = null

    for(i in 0..(currentList.size-1)) {
      val beacon = currentList.get(i)


      Log.d("ActivityUpdate", "Element is: ${beacon.eddystoneUidData?.namespaceId}")
      var uid = beacon.eddystoneUidData?.namespaceId
      for(beaconlist in room){
        if(beaconlist.m_beaconuidList.contains(uid)) {
          count_room[room.indexOf(beaconlist)]++
          if (closestBeacon == null) {
            closestBeacon = room.indexOf(beaconlist)
            Log.d("ActivityUpdate", "Closest beacon in room ${room.indexOf(beaconlist)}")
          }
        }
      }


    }


    count_room.maxOrNull().also {
      var max:Int? = count_room.indexOf(it)
      count_room.sortDescending().also {
        //If room count of multiple rooms is same, then choose closest beacon
        if (count_room[0] == count_room[1] && closestBeacon != null)
          max = closestBeacon


        Log.d("ActivityUpdate", "Room count = {$max}")
        if (max == 0) viewModel.location.postValue(SupportedLocation.LUNCH)
        else if (max == 1) viewModel.location.postValue(SupportedLocation.LECTURE)
        else if (max == 2) viewModel.location.postValue(SupportedLocation.MEETING)
        else if (max == 3) viewModel.location.postValue(SupportedLocation.MEETING)
        else if (max == 4) viewModel.location.postValue(SupportedLocation.COMMON)

        if (getDetectedActivity() == SupportedActivity.WALKING && (max != 1)) {
          return OfficeActivity.STATE_EXERCISE
        } else if (max == 0) {
          return OfficeActivity.STATE_LUNCH
        } else if (max == 2 || max == 3) {
          return OfficeActivity.STATE_MEETING
        } else if (max == 4) {
          return OfficeActivity.STATE_SOCIALISE
        } else
          return OfficeActivity.STATE_WORK
      }

    }
    Log.d("ActivityUpdate","identifyRoom")


    return OfficeActivity.STATE_WORK


  }
  private fun initPlotXY()
  {
    //Sample XY plot
    spinner?.setVisibility(View.GONE);
    linechart?.setVisibility(View.VISIBLE);
    val vAxisValues = mutableListOf<AxisValue>()
    for (i in 0 until (yaxisData.size)) {
      vAxisValues.add(AxisValue(i.toFloat()).setLabel(yaxisData[i].toString()))

      //yAxisValues.add(PointValue(i.toFloat(), 3.0F))
    }

    val data = LineChartData()


    val axis = Axis().setLineColor(Color.parseColor("#9C27B0"))
    val yAxis = Axis().setLineColor(Color.parseColor("#9C27B0"))

    yAxis.setValues(vAxisValues)
    yAxis.setTextColor(Color.parseColor("#9C27B0"))
    axis.setTextColor(Color.parseColor("#9C27B0"))
    data.setAxisXBottom(axis)
    data.setAxisYRight(yAxis)
    data.setValueLabelsTextColor(Color.parseColor("#9C27B0"))
    data.setValueLabelTextSize(10)
    val numValues = 5

    val values: MutableList<PointValue> = ArrayList(numValues)
    values.add(PointValue(0F, 0F))
    values.add(PointValue(0F, 1F))
    values.add(PointValue(0F, 2F))
    values.add(PointValue(0F, 3F))
    values.add(PointValue(0F, 4F))

    val line1 = Line(values)
    line1.setPointRadius(0)
    lines.add(line1)

    val line2 = Line(yAxisValues).setColor(Color.parseColor("#9C27B0"))
    lines.add(line2)
    data.lines = lines
    linechart?.setLineChartData(data);


  }


  private fun plotxy(act: DetectActivity.OfficeActivity)
  {


    val vAxisValues = mutableListOf<AxisValue>()
    for (i in 0 until (yaxisData.size)) {
      vAxisValues.add(AxisValue(i.toFloat()).setLabel(yaxisData[i]))
      //yAxisValues.add(PointValue(i.toFloat(), 3.0F))
    }

    var i=yAxisValues.size
    yAxisValues.add(PointValue(i.toFloat(), act.ordinal.toFloat()))
    Log.d("ActivityUpdate","$i points found")
    var j = axisValues.size
    val sdf = SimpleDateFormat("HH:mm")
    val currentTime: String = sdf.format(Date())

    axisValues.add(AxisValue(j.toFloat()).setLabel(currentTime))

    val data = LineChartData()
    data.setLines(lines)

    val axis = Axis().setLineColor(Color.parseColor("#9C27B0"))
    val yAxis = Axis().setLineColor(Color.parseColor("#9C27B0"))
    yAxis.setValues(vAxisValues)
    yAxis.setTextColor(Color.parseColor("#9C27B0"))
    axis.setTextColor(Color.parseColor("#9C27B0"))
    axis.setValues(axisValues)
    data.setAxisXBottom(axis)
    data.setAxisYRight(yAxis)


    linechart?.setLineChartData(data);


  }

  private fun disableStopBtn(){
    stopBtn?.isEnabled = false
    stopBtn?.setBackground(getResources().getDrawable(R.drawable.rounded_disabled_btn));
  }

  private fun enableStopBtn(){
    stopBtn?.isEnabled = true
    stopBtn?.setBackground(getResources().getDrawable(R.drawable.rounded_button));
  }

  private fun disableStartBtn(){
    startBtn?.isEnabled = false
    startBtn?.setBackground(getResources().getDrawable(R.drawable.rounded_disabled_btn));
  }

  private fun enableStartBtn(){
    startBtn?.isEnabled = true
    startBtn?.setBackground(getResources().getDrawable(R.drawable.rounded_button));
  }



  private fun initialize() {
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager;


    linechart = findViewById(R.id.chart)
    stepCounter = 0
    prevSteps = 0

    startBtn =  findViewById(R.id.startBtn);
    stopBtn =  findViewById(R.id.stopBtn);
    btnReset = findViewById(R.id.resetBtn);
    btnReset?.setVisibility(View.INVISIBLE);
    activityImage = findViewById(R.id.activityImage);
    activityTitle = findViewById(R.id.activityTitle);
    locImage = findViewById(R.id.geoImage);
    locTitle = findViewById(R.id.geoTitle);


    spinner = findViewById(R.id.progressBar);
    spinner?.setVisibility(View.GONE);
    linechart?.setVisibility(View.GONE);
    disableStopBtn()

    }

  /* Calculate the acceleration and steps when sensor values are updated */
  override fun onSensorChanged(event: SensorEvent?) {
      //Log.d("ActivityUpdate", "Abby sensor chg detected ${event?.sensor?.name}")
        if (event?.sensor == stepSensor && isScanning)
        {

          //txtCheck?.setText(event?.sensor.getText() + System.getProperty("line.separator") + "Sensorevent was triggered!");
            if(event?.values != null){
              var steps:Int = event?.values[0].toInt()
              totSteps += steps
              if(prevSteps !=0)
              {
                stepCounter = (stepCounter + (steps - prevSteps))

              }
              prevSteps = steps
              //var step:String =  event?.values[0].toString()
            }

          Log.d("ActivityUpdate", " count steps = $stepCounter")
        }
      if (event?.sensor == moveSensor && isScanning) {
        mGravity = event?.values?.clone()?.toList();

        /* Introduced local variable to fix:
        Smart cast to 'List<Float>' is impossible,
        because 'mGravity' is a mutable property that could have
        been changed by this time */

        var gravity:List<Float>? = mGravity;
        if(gravity != null){
          var x = gravity.get(0).toDouble();
          var y  = gravity.get(1).toDouble();
          var z  = gravity.get(2).toDouble();
          mAccelLast = mAccelCurrent;
          mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
          var delta:Double  = mAccelCurrent - mAccelLast;
          mAccel = mAccel * 0.9f + delta;

          if (hitCount <= SAMPLE_SIZE) {
            hitCount++;
            hitSum += Math.abs(mAccel);
          } else {
            hitResult = hitSum / SAMPLE_SIZE;

            Log.d("ActivityUpdate", hitResult.toString());

            if (hitResult > THRESHOLD) {
              if (prevStepCounter < stepCounter) {
                Log.d("ActivityUpdate", "Walking");
                viewModel.activity.postValue(SupportedActivity.RUNNING)
                prevStepCounter = stepCounter
              }
              else {
                Log.d("ActivityUpdate", "Moving but not Walking");
                viewModel.activity.postValue(SupportedActivity.WALKING)
              }

            } else {
              Log.d("ActivityUpdate", "Stop Walking");
              viewModel.activity.postValue(SupportedActivity.STILL)
            }

            hitCount = 0;
            hitSum = 0.0;
            hitResult = 0.0;
          }
        }

    }

  }
  fun isScanning() = isScanning

  private fun unbindBeaconManager() {
    if (beaconManager?.isBound(this) == true) {
      Timber.d("Unbinding from beaconManager")
      beaconManager?.unbind(this)
    }
  }

  /* Reset view on pressing 'Reset button' */
  private fun resetTracking() {
    isTrackingStarted = false
    setDetectedActivity(SupportedActivity.NOT_STARTED)
    setDetectedLocation(SupportedLocation.NOT_STARTED)
    //removeActivityTransitionUpdates()
    //stopService(Intent(this, DetectedActivityService::class.java))
    stopScan()
    yAxisValues.clear()
    initPlotXY()
  }


  override fun onResume() {
    super.onResume()
    registerReceiver(transitionBroadcastReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
    Log.d("ActivityUpdate", "Abby onResume")
    /* Indoor geolocation */

    Timber.d("onResume scanning");
    beaconManager?.setBackgroundMode(false);
    //startScan()
    if(isScanning) {
        Log.d("ActivityUpdate","Still scanning")

    }

  }


  /* After every beacon scanning, store the beacons found in the database */
  private fun storeBeaconsAround(beacons: Collection<Beacon>) {
    Log.d("ActivityUpdate","storeBeaconsAround")
    Log.d("ActivityUpdate",beacons.toString())

    if(!beacons.isEmpty())
    {
      Observable.fromIterable(beacons)
        .filter{
          it.serviceUuid == 0xFEAA  //Eddystone UID format
                  && it.id1.toString() != GEOFENCE_BEACON_ID //Beacon used for geofencing
        }
        .map {

          BeaconSaved.createFromBeacon(it, isBlocked = false)

        }
        //.delay(10000L, TimeUnit.MILLISECONDS)
        .doOnNext {
          Log.d("ActivityUpdate","Store beacon data")
          db.insertBeacon(it)
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          Timber.d("Beacon inserted")
        }, { err ->
          Timber.e(err)
          //showGenericError(err?.message ?: "")
        }).also{
          if(!firstScanDone) {
            firstScanDone = true //Initialise the line chart on first beacon scan
            initPlotXY()
          }
        }
    }


  }

  /* Register listeners for beacon ranging */
  override fun onBeaconServiceConnect() {
    Log.d("ActivityUpdate","onBeaconServiceConnect");
    beaconManager?.addRangeNotifier {
        beacons, _ ->
      if (isScanning) {
        storeBeaconsAround(beacons)
      }
    }

    try {
      Log.d("ActivityUpdate","Start monitoring region")

      /* Range all beacons found */
      var region2 = Region("com.example.retraceworkplace", null, null, null)
      beaconManager?.startRangingBeaconsInRegion(region2)
    } catch (e: RemoteException) {
      Log.d("ActivityUpdate","Error ranging beacons")
      e.printStackTrace()
    }
  }


  override fun onPause() {
    //unregisterReceiver(transitionBroadcastReceiver)
    super.onPause()
    Log.d("ActivityUpdate", "onPause")

    beaconManager?.setBackgroundMode(true);


  }

  override fun onDestroy() {
    removeActivityTransitionUpdates()
    stopService(Intent(this, DetectedActivityService::class.java))
    super.onDestroy()
  }


  private fun setDetectedActivity(supportedActivity: SupportedActivity) {
    activityImage?.setImageDrawable(ContextCompat.getDrawable(this, supportedActivity.activityImage))
    activityTitle?.text = getString(supportedActivity.activityText)



    if(supportedActivity == SupportedActivity.STILL && simpleActivity == supportedActivity)
    {
      Log.d("ActivityUpdate","Set supported activity : SLOW")
      beaconManager?.foregroundScanPeriod = 1100L
      beaconManager?.foregroundBetweenScanPeriod = 100000L

    }
    else
    {
      Log.d("ActivityUpdate","Set supported activity : FAST")
      beaconManager?.foregroundScanPeriod = 600L
      beaconManager?.foregroundBetweenScanPeriod = 2000L
    }
    simpleActivity = supportedActivity

  }

  private fun setDetectedLocation(supportedlocation: SupportedLocation) {
    Log.d("ActivityUpdate","Set supported location")
    locImage?.setImageDrawable(ContextCompat.getDrawable(this, supportedlocation.locImage))
    locTitle?.text = getString(supportedlocation.locText)

  }



  private fun  getDetectedActivity()
  : SupportedActivity? {
    return simpleActivity
  }


  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACTIVITY_RECOGNITION).not() &&
        grantResults.size == 1 &&
        grantResults[0] == PackageManager.PERMISSION_DENIED) {
      showSettingsDialog(this)
    } else if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION &&
        permissions.contains(Manifest.permission.ACTIVITY_RECOGNITION) &&
        grantResults.size == 1 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d("permission_result", "permission granted")
      startService(Intent(this, DetectedActivityService::class.java))
      requestActivityTransitionUpdates()
      isTrackingStarted = true
    }
  }
}


class BeaconRow() {
  data class Beacon(val beacon: BeaconSaved)
}

