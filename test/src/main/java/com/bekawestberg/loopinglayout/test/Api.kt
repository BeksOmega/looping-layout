package com.bekawestberg.loopinglayout.test

import retrofit2.http.GET
import retrofit2.http.Query

interface Api {
    @GET("v2/list")
    suspend fun getPhotos(): List<NetItem>
}