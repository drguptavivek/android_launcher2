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

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val user: UserData?,
    val error: String?
)

data class UserData(
    val id: String,
    val username: String,
    val role: String
)

interface ApiService {
    @POST("/api/devices/register")
    suspend fun registerDevice(@Body request: RegistrationRequest): RegistrationResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
