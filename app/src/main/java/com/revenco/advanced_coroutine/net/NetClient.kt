package com.revenco.advanced_coroutine.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 *  Copyright © 2021/4/5 Hugecore Information Technology (Guangzhou) Co.,Ltd. All rights reserved.
 *  author: chenqi
 */
class NetClient {


    companion object {
        private var instance: NetClient? = null

        fun getClient(): NetClient {
            instance = instance ?: synchronized(NetClient::class.java)
            {
                instance ?: synchronized(NetClient::class.java)
                {
                    NetClient().also {
                        instance = it
                    }
                }
            }
            return instance!!
        }
    }

    private val httpLoggingInterceptor = HttpLoggingInterceptor().also {
        it.level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okHttp)
        .baseUrl("https://www.wanandroid.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun createNetApi(): NetApi {
        return retrofit.create(NetApi::class.java)
    }
}