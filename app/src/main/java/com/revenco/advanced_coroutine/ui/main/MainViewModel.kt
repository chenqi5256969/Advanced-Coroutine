package com.revenco.advanced_coroutine.ui.main

import ControlledRunner
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.revenco.advanced_coroutine.net.BannerBean
import com.revenco.advanced_coroutine.net.NetClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    val controlledRunner = ControlledRunner<BannerBean>()

    fun getBanner() {
        viewModelScope.launch { withContext(Dispatchers.IO) {
                controlledRunner.joinPreviousOrRun {
                    delay(2000)
                    NetClient.getClient().createNetApi().getHomeBanner()
                }
            }
        }
    }
}