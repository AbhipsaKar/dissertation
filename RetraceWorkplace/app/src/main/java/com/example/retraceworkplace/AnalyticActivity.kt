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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.retraceworkplace.beaconscanner.Database.AnalyticsDao
import com.example.retraceworkplace.beaconscanner.Database.AppDatabase
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved

import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class AnalyticActivity: AppCompatActivity() {

  /* View component variables */
  private var workValue:TextView? = null
  private var socValue:TextView? = null
  private var totTimeValue:TextView? = null
  private var lunchTimeValue:TextView? = null
  private var meetingTimeValue:TextView? = null
  private var exerciseTimeValue:TextView? = null
  private var stepsValue:TextView? = null

  private lateinit var viewModel: AnalyticsViewModel
  private lateinit var viewModelFactory: AnalyticsViewModelFactory
  private lateinit var calendar:CalendarView
  @Inject
  lateinit var db: AnalyticsDao

  /* Bottom navigation bar */
  private val navListener =
    BottomNavigationView.OnNavigationItemSelectedListener { item -> // By using switch we can easily get
      // the selected fragment
      // by using there id.
      var selectedFragment: Fragment? = null
      when (item.itemId) {
        R.id.Home -> {
          Home()
          val intent = Intent (this.applicationContext, MainActivity::class.java)
          intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //Do not stop activity. Just reorder
          startActivity(intent)
        }
        //R.id.Tracking -> selectedFragment = true
        R.id.Tracking -> {
          Tracking()
          val intent = Intent (this.applicationContext, DetectActivity::class.java)
          intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
          startActivity(intent)
        }
      }


      true
    }

  private fun observeViewModel( viewModel: AnalyticsViewModel) {
    viewModel.historyActivity.observe(this,  androidx.lifecycle.Observer {

      if (it != null) {
        Log.d("ActivityUpdate","Live data on changed : update ")
        totTimeValue?.setText("${it.total_time} sec");
        lunchTimeValue?.setText("${it.lunch_time} sec");
        meetingTimeValue?.setText("${it.meeting_time} sec");
        socValue?.setText("${it.socialise_time} sec");
        exerciseTimeValue?.setText("${it.walking_time} sec");
        workValue?.setText("${it.work_time} sec");
        stepsValue?.setText("${it.steps}");

      }
      else{
        Log.d("ActivityUpdate","Live data on changed : set to default")
        totTimeValue?.setText("0.00 sec");
        lunchTimeValue?.setText("0.00 sec");
        meetingTimeValue?.setText("0.00 sec");
        exerciseTimeValue?.setText("0.00 sec");
        socValue?.setText("0.00 sec");
        workValue?.setText("0.00 sec");
        stepsValue?.setText("0");
      }

    });
  }


  override fun onCreate(savedInstanceState: Bundle?) {


    db = AppDatabase.getDatabase(application).analyticsDao()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.fragment_history)
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
    bottomNav.setOnNavigationItemSelectedListener(navListener)
    bottomNav.setSelectedItemId(R.id.History);
    // as soon as the application opens the first
    // fragment should be shown to the user
    // in this case it is algorithm fragment


    initialize();

    viewModelFactory = AnalyticsViewModelFactory()
    viewModel = ViewModelProvider(this, viewModelFactory).get(AnalyticsViewModel::class.java)

    observeViewModel(viewModel);

    /* Update live data on initialise */
    GlobalScope.launch {
      repeat(1) {
        Log.d("ActivityUpdate", "CorRoutine analytics")
        var data: AnalyticsSaved =
          db.getAnalyticsForDay(day = Date().getDate(), month = Date().month, year = Date().year,)
        Log.d("ActivityUpdate", "[data = $data]")
        viewModel.historyActivity.postValue(data)
      }
      delay(100000) // Suspends the coroutine for some time
    }


    }

  override fun onResume() {
    super.onResume()
    GlobalScope.launch {
      repeat(1) {
        Log.d("ActivityUpdate", "resume analytics")
        var data: AnalyticsSaved =
          db.getAnalyticsForDay(day = Date().getDate(), month = Date().month, year = Date().year,)
        //Log.d("ActivityUpdate", "print today's data")
        Log.d("ActivityUpdate", "[data = $data]")
        viewModel.historyActivity.postValue(data)
      }
      delay(100000) // Suspends the coroutine for some time
    }
  }

  /* Initialise view components */
   private fun initialize(){
    calendar = findViewById(R.id.calendarView2)
    calendar.setOnDateChangeListener(CalendarView.OnDateChangeListener { _, year, mon, day ->
      //val date = "$i/$il/$i2"
      GlobalScope.launch {
        repeat(1) {
          Log.d("ActivityUpdate", "Hello analytics")
          var data: AnalyticsSaved =
            db.getAnalyticsForDay(day = day, month = mon, year = Date().year,)
          //Log.d("ActivityUpdate", "print today's data")
          Log.d("ActivityUpdate", "[data = $data]")
          viewModel.historyActivity.postValue(data)
        }
        delay(100000) // Suspends the coroutine for some time
      }

        //Log.d("ActivityUpdate","Date is $date")

    })

     totTimeValue = findViewById(R.id.totTimeValue);
    socValue = findViewById(R.id.socTimeValue);
     workValue = findViewById(R.id.workTimeValue);
    lunchTimeValue = findViewById(R.id.lunchTimeValue);
    meetingTimeValue = findViewById(R.id.meetingTimeValue);
    exerciseTimeValue = findViewById(R.id.exerciseTimeValue);
    stepsValue = findViewById(R.id.stepsValue);
    }



  }






