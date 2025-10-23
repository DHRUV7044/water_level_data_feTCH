package com.example.water_level_data_fetch

import retrofit2.http.GET

interface WaterLevelApiService {
    @GET("height")
    suspend fun getWaterLevelData(): WaterLevelData
}
