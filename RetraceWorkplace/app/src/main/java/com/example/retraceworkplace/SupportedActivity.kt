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

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.DetectedActivity
import java.lang.IllegalArgumentException


const val SUPPORTED_ACTIVITY_KEY = "activity_key"

enum class SupportedActivity(
    @DrawableRes val activityImage: Int,
    @StringRes val activityText: Int
) {

  NOT_STARTED(R.drawable.time_to_start, R.string.time_to_start),
  STILL(R.drawable.sitting, R.string.still_text),
  WALKING(R.drawable.moving, R.string.walking_text),
  RUNNING(R.drawable.walking, R.string.running_text);

  companion object {

    fun fromActivityType(type: Int): SupportedActivity = when (type) {
      DetectedActivity.STILL -> STILL
      DetectedActivity.WALKING -> WALKING
      DetectedActivity.RUNNING -> RUNNING
      else -> throw IllegalArgumentException("activity $type not supported")
    }
  }
}

/* View model for the Tracking page */
class CurrentActivityViewModel() : ViewModel() {

  val location:MutableLiveData<SupportedLocation> by lazy {
    MutableLiveData<SupportedLocation>()
  }

  val activity:MutableLiveData<SupportedActivity> by lazy {
    MutableLiveData<SupportedActivity>()
  }

  val currentActivity: MutableLiveData<DetectActivity.OfficeActivity> by lazy {
    MutableLiveData<DetectActivity.OfficeActivity>()
  }


  init {
    currentActivity.value = DetectActivity.OfficeActivity.STATE_WORK
    location.value = SupportedLocation.NOT_STARTED
    activity.value = SupportedActivity.NOT_STARTED
  }

  fun getLiveData(): MutableLiveData<DetectActivity.OfficeActivity> {
    return currentActivity
  }

  fun getLocData(): MutableLiveData<SupportedLocation> {
    return location
  }

  fun getActData(): MutableLiveData<SupportedActivity> {
    return activity
  }

  fun updateActivity(act : DetectActivity.OfficeActivity){
    Log.d("UpdateActivity","update again")
      currentActivity.value = act

  }

}

class CurrentActivityViewModelFactory() : ViewModelProvider.Factory{

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CurrentActivityViewModel::class.java)){
      return CurrentActivityViewModel() as T
    }
    throw IllegalArgumentException("Unknown View Model Class")
  }


}