package com.wittyneko.neteasemusiccache

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiInterface {
    @GET("cloudmusic/")
    fun cloudMusic(@QueryMap query: Map<String, String>): Call<JsonElement>

    @GET("cloudmusic/")
    fun getDetail(@Query("id") id: String, @Query("type") type: String = "detail"): Call<JsonElement>

    @GET("cloudmusic/")
    fun getLyric(@Query("id") id: String, @Query("type") type: String = "lyric"): Call<JsonElement>
}