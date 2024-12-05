package com.travelsketch.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    // 기존 latlng 기반 요청
    @GET("geocode/json")
    suspend fun getGeocoding(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): GeocodingResponse

    @GET("geocode/json")
    suspend fun getGeocodingByAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}

data class GeocodingResponse(
    val results: List<GeocodingResult>
)

data class GeocodingResult(
    val formatted_address: String,
    val address_components: List<AddressComponent>,
    val geometry: Geometry // geometry 추가
)

data class Geometry(
    val location: Location // location 필드 추가
)

data class Location(
    val lat: Double, // 위도
    val lng: Double  // 경도
)

data class AddressComponent(
    val long_name: String,
    val short_name: String,
    val types: List<String> // types 배열로 주소의 유형을 정의
)

object RetrofitInstance {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

    val geocodingService: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }
}
