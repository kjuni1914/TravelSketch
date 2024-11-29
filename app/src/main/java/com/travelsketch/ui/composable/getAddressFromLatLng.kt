package com.travelsketch.ui.composable

import com.google.android.gms.maps.model.LatLng
import com.travelsketch.api.RetrofitInstance

suspend fun getAddressFromLatLng(position: LatLng, apiKey: String): String {
    val latlng = "${position.latitude},${position.longitude}"
    val response = RetrofitInstance.geocodingService.getGeocoding(latlng, apiKey)

    if (response.results.isNotEmpty()) {
        val addressComponents = response.results[0].address_components
        val country = addressComponents.find { "country" in it.types }?.long_name ?: "국가 정보 없음"
        val city = addressComponents.find { "locality" in it.types || "administrative_area_level_1" in it.types }
            ?.long_name ?: "도시 정보 없음"
        val district = addressComponents.find { "sublocality" in it.types || "administrative_area_level_2" in it.types }
            ?.long_name ?: "동 정보 없음"

        return "$country, $city, $district"
    } else {
        return "주소를 찾을 수 없습니다."
    }
}
