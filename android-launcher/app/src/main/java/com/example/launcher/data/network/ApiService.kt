package com.example.launcher.data.network

import retrofit2.http.Body
import retrofit2.http.POST

data class RegistrationRequest(
    val registrationCode: String,
    val model: String,
    val androidVersion: String
)

data class RegistrationResponse(
    val status: String,
    val message: String,
    val deviceId: String,
    val description: String,
    val registeredAt: String
)

data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String
)

data class LogoutRequest(
    val userId: String,
    val deviceId: String
)

data class LoginResponse(
    val success: Boolean,
    val user: UserData?,
    val error: String?
)

data class UserData(
    val id: String,
    val username: String,
    val role: String,
    val loginTime: Long = System.currentTimeMillis()
)

data class TelemetryEvent(
    val type: String,
    val data: Any,
    val timestamp: Long
)

data class TelemetryRequest(
    val userId: String,
    val deviceId: String,
    val events: List<TelemetryEvent>
)

data class TelemetryResponse(
    val success: Boolean,
    val count: Int?,
    val error: String?
)

interface ApiService {
    @POST("/api/devices/register")
    suspend fun registerDevice(@Body request: RegistrationRequest): RegistrationResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): LoginResponse

    @POST("/api/telemetry")
    suspend fun sendTelemetry(@Body request: TelemetryRequest): TelemetryResponse
}
