package com.example.launcher.data.network

import retrofit2.http.Body
import retrofit2.http.POST

data class RegistrationRequest(
    val id: String,
    val model: String,
    val androidVersion: String
)

data class RegistrationResponse(
    val status: String,
    val message: String
)

interface ApiService {
    @POST("/api/devices/register")
    suspend fun registerDevice(@Body request: RegistrationRequest): RegistrationResponse
}
