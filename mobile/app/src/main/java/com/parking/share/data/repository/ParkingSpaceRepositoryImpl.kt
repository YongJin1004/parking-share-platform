package com.parking.share.data.repository

import android.content.Context
import android.net.Uri
import com.parking.share.data.remote.api.ParkingSpaceApi
import com.parking.share.data.remote.dto.ParkingSpaceCreateRequest
import com.parking.share.data.remote.dto.ScheduleItemDto
import com.parking.share.data.remote.dto.toDomain
import com.parking.share.domain.model.ParkingSpace
import com.parking.share.domain.repository.ParkingSpaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ParkingSpaceRepositoryImpl @Inject constructor(
    private val parkingSpaceApi: ParkingSpaceApi,
    @ApplicationContext private val context: Context
) : ParkingSpaceRepository {

    override suspend fun getMyParkingSpaces(): Result<List<ParkingSpace>> {
        return try {
            val response = parkingSpaceApi.getMyParkingSpaces()
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createParkingSpace(
        address: String,
        hourlyRate: Int?,
        description: String?,
        availableSchedule: List<ScheduleItemDto>?,
        allowedVehicleTypes: List<String>?
    ): Result<ParkingSpace> {
        return try {
            val request = ParkingSpaceCreateRequest(
                address = address,
                description = description,
                availableSchedule = availableSchedule,
                allowedVehicleTypes = allowedVehicleTypes
            )
            val response = parkingSpaceApi.createParkingSpace(request)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImages(spaceId: Int, imageUris: List<Uri>): Result<Unit> {
        return try {
            val parts = imageUris.mapIndexedNotNull { index, uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapIndexedNotNull null
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", "image_$index.$ext", requestBody)
            }
            if (parts.isNotEmpty()) {
                parkingSpaceApi.uploadImages(spaceId, parts)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteParkingSpace(id: Int): Result<Unit> {
        return try {
            parkingSpaceApi.deleteParkingSpace(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchParkingSpaces(
        keyword: String?,
        minHourlyRate: Int?,
        maxHourlyRate: Int?,
        isAvailable: Boolean?
    ): Result<List<ParkingSpace>> {
        return try {
            val response = parkingSpaceApi.searchParkingSpaces(
                keyword = keyword,
                minHourlyRate = minHourlyRate,
                maxHourlyRate = maxHourlyRate,
                isAvailable = isAvailable
            )
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
