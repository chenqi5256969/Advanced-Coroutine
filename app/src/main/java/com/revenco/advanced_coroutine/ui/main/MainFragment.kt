package com.revenco.advanced_coroutine.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.revenco.advanced_coroutine.R
import java.util.concurrent.atomic.AtomicInteger

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    private val count = AtomicInteger()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val netBtn = view.findViewById<Button>(R.id.message)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        netBtn.setOnClickListener {
            viewModel.getBanner()
        }
    }

}