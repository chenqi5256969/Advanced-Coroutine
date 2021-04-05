package com.revenco.advanced_coroutine.net

import retrofit2.http.GET

/**
 *  Copyright © 2021/4/5 Hugecore Information Technology (Guangzhou) Co.,Ltd. All rights reserved.
 *  author: chenqi
 */

interface NetApi {

    @GET("banner/json")
    suspend fun getHomeBanner(): BannerBean

}