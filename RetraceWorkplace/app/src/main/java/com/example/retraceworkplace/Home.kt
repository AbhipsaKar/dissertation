package com.example.retraceworkplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.retraceworkplace.beaconscanner.models.AnalyticsSaved
import java.lang.IllegalArgumentException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment() {
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val intent = Intent (this@Home.context, MainActivity::class.java)
        startActivity(intent)
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Home.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
/* View model for the Home page */
class HomeActivityViewModel() : ViewModel() {

    val office_state: MutableLiveData<MainActivity.RegionState> by lazy {
        MutableLiveData<MainActivity.RegionState>()
    }

    val avgSteps: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val totTime: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }



    init {
        office_state.value = MainActivity.RegionState.STATE_OUTSIDE

    }

    fun getOfficeState(): MutableLiveData<MainActivity.RegionState> {
        return office_state
    }


}

class HomeActivityViewModelFactory() : ViewModelProvider.Factory{

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeActivityViewModel::class.java)){
            return HomeActivityViewModel() as T
        }
        throw IllegalArgumentException("Unknown View Model Class")
    }


}