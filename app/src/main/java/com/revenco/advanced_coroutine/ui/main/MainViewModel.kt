package com.revenco.advanced_coroutine.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenco.advanced_coroutine.net.NetClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    fun getBanner() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(3000)
                NetClient.getClient().createNetApi().getHomeBanner()
            }
        }
    }
}