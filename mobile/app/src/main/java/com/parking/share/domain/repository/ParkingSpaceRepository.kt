package com.parking.share.domain.repository

import android.net.Uri
import com.parking.share.data.remote.dto.ScheduleItemDto
import com.parking.share.domain.model.ParkingSpace

interface ParkingSpaceRepository {
    suspend fun getMyParkingSpaces(): Result<List<ParkingSpace>>
    suspend fun createParkingSpace(
        address: String,
        hourlyRate: Int?,
        description: String?,
        availableSchedule: List<ScheduleItemDto>?,
        allowedVehicleTypes: List<String>?
    ): Result<ParkingSpace>
    suspend fun uploadImages(spaceId: Int, imageUris: List<Uri>): Result<Unit>
    suspend fun deleteParkingSpace(id: Int): Result<Unit>
    suspend fun searchParkingSpaces(
        keyword: String? = null,
        minHourlyRate: Int? = null,
        maxHourlyRate: Int? = null,
        isAvailable: Boolean? = true
    ): Result<List<ParkingSpace>>
}
