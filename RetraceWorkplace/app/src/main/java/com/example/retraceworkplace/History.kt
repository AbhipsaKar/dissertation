package com.example.retraceworkplace

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.EmptyResultSetException
import com.example.retraceworkplace.beaconscanner.Database.AnalyticsDao
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import kotlinx.coroutines.launch
import java.time.Month
import java.time.Year
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [History.newInstance] factory method to
 * create an instance of this fragment.
 */
class History : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
/*
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }*/

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment History.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            History().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

/* View model for the Analytics page */
class AnalyticsViewModel() : ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val historyActivity: MutableLiveData<AnalyticsSaved> by lazy {
        MutableLiveData<AnalyticsSaved>()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getDbData(db: AnalyticsDao, year: Int, month: Int, date: Int) {

        var data: AnalyticsSaved =
            db.getAnalyticsForDay(day =date, month = month, year = year)

        Log.d("ActivityUpdate", "[data = $data]")
        if(data != null)
        {
            historyActivity.postValue(data)
        }
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun getData(db: AnalyticsDao, year: Int, month: Int, date: Int) = viewModelScope.launch {
        while (db!=null) {
            Log.d("ActivityUpdate", "Get Data")
            getDbData(db,year, month, date)
        }
    }
}

class AnalyticsViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}