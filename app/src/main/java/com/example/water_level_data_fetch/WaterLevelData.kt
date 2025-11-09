package com.example.water_level_data_fetch

import com.google.gson.annotations.SerializedName

data class WaterLevelData(
    @SerializedName("water_level_mm")
    val waterLevelMm: Double
)
