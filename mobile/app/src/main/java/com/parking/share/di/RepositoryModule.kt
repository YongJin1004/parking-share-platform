package com.parking.share.di

import com.parking.share.data.repository.AuthRepositoryImpl
import com.parking.share.data.repository.ParkingSpaceRepositoryImpl
import com.parking.share.domain.repository.AuthRepository
import com.parking.share.domain.repository.ParkingSpaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindParkingSpaceRepository(
        parkingSpaceRepositoryImpl: ParkingSpaceRepositoryImpl
    ): ParkingSpaceRepository
}
